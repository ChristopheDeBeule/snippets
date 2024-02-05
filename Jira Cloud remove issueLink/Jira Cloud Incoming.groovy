// import jsonSlurper
import groovy.json.JsonSlurper
// This will add the subtasks as well
if(firstSync && replica.parentId){
    issue.typeName = "Sub-task"
    def localParent = nodeHelper.getLocalIssueFromRemoteId(replica.parentId.toLong())
    //debug.error(localParent.toString())
	if(localParent){
		issue.parentId = localParent.id
	} else {
      throw new com.exalate.api.exception.IssueTrackerException("Subtask cannot be created: parent issue with remote id " + replica.parentId + " was not found. Please make sure the parent issue is synchronized before resolving this error" )
    }
}

// Check if a link exists in a list of links
def contains(link, remoteLinks) {
    for(_link in remoteLinks) {
        def localLinkId = nodeHelper.getLocalIssueKeyFromRemoteId(_link.otherIssueId).id
        if(localLinkId == link.otherIssueId) {
            return true
        }
    }
    return false
}

// A list that will hold all links we wish to delete
def toDelete = []
// We loop over the replica issueLinks and resolve all links to a local issue key

def issueLinks = []
if ((replica.issueLinks.isEmpty()) && (!previous.issueLinks.isEmpty())){
    issueLinks = previous.issueLinks
}else{
    issueLinks = replica.issueLinks
}

for(remoteLink in issueLinks) {
    def localIssueId = nodeHelper.getLocalIssueKeyFromRemoteId(remoteLink.otherIssueId).id

    // If the local issue link is not present in the remote, then we should delete it
    for(link in issue.issueLinks) {
        if(!contains(link, replica.issueLinks)) {
            toDelete += link // so we add it to the list of items we wish to delete
        }
    }

}
// sett issue.issueLinks = replica.issueLinks
issue.issueLinks = replica.issueLinks

def getLocalIssueLinksId(otherId){
    def restApiCall = httpClient.get("/rest/api/3/issue/${issue.key}") 
    def jsonText = new groovy.json.JsonOutput().toJson(restApiCall) 
    def jsonParsed = new JsonSlurper().parseText(jsonText)
    
    if (jsonParsed == null){
        return
    }

    def issueLinks = jsonParsed.fields.issuelinks

    for (int i = 0; i < issueLinks.size(); i++) {
        // TODO: check that inwardIssue exist if not use outwardIssue
        if(issueLinks[i]?.inwardIssue != null && issueLinks[i].inwardIssue.id == otherId.toString()) {
            return issueLinks[i].id
        }
        if(issueLinks[i]?.outwardIssue != null && issueLinks[i].outwardIssue.id == otherId.toString()) {
            return issueLinks[i].id
        }
    }

    return
}

def deleteRequest(path) {
    path = path.toString()
    def responseJson = (new JiraClient(httpClient)).http(
        "DELETE", 
        path,
        ["sysparm_display_value": ["true"]],
        null,
        ["Content-Type":["application/json"]]
    ) { response -> 
        if (response.code >= 400) debug.error ("DELETE ${path} failed: ${response.code} ${response.body}".toString())
        else { if (response.body != null && !response.body.empty) (new JsonSlurper()).parseText(response.body) else null }
    }   
}

// delete an issue link
def delete(link) {
    def otherIssueId = link.otherIssueId
    def issueLinkId = getLocalIssueLinksId(otherIssueId)

    if(issueLinkId == null) {
        debug.error("Unable to find issue link ${issueLinkId} for issue '${issue.key}'")
    }

    deleteRequest("/rest/api/3/issueLink/${issueLinkId}") 
}

// loop over all issuelinks we wish to delete, and delete them
for (link in toDelete) {
    delete(link)
}
