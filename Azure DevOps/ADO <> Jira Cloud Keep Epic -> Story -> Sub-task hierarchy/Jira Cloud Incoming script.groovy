import groovy.json.JsonOutput 

def setParentField(def remoteParentId, String parentType = "Epic"){
  if (!remoteParentId) return
  store(issue)
  def localIssue = nodeHelper.getLocalIssueFromRemoteId(replica.parentId)
  def localParentType = httpClient.get("/rest/api/3/issue/${localIssue?.key}")?.fields?.issuetype?.name

  if(localIssue && localParentType == parentType){
    issue.parentId = localIssue.id
  }else if(localIssue){
    // Link the issues with each other 
    def bodyLinkIssues = [
      "outwardIssue": [
        "key": "${localIssue.key}"
      ],
      "inwardIssue": [
        "key": "${issue.key}"
      ],
      "type": [
        "name": "Relates"
      ]
    ]
    // Link the new issue to the parent from ADO
    httpClient.post("/rest/api/3/issueLink", JsonOutput.toJson(bodyLinkIssues))
  }
}

if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   def typeMap = [
      // "ADO side":"Jira Cloud side"
      "Epic":"Epic",
      "Feature":"Feature",
      "User Story":"Story"
   ]
   issue.typeName     = nodeHelper.getIssueType(typeMap[replica.type?.name], issue.projectKey)?.name ?: nodeHelper.getIssueType(replica.type?.name, (issue.projectKey ?: issue.project.key))?.name ?: "Task"
   issue.summary      = replica.summary
}
// This will check if the issueType is a Feature and if there is a parentId now it will link the epic with the feature.
// And it will only link the issues as a child issue under the feature
// NOTE: Change the issueType to the type that needs the parent link.
if (replica.parentId) {
  setParentField(replica.parentId)
}

issue.summary      = replica.summary
issue.description  = replica.description
issue.comments     = commentHelper.mergeComments(issue, replica)
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.labels       = replica.labels
