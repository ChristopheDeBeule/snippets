import groovy.json.JsonOutput

// Function to do PATCHes
def linkIssuesPatch(String baseUrl, def body, String projectKey, def token, def workItemId){
    
    def createIterationBodyStr = JsonOutput.toJson(body)
    converter = scala.collection.JavaConverters;
    arrForScala = [new scala.Tuple2("Content-Type","application/json-patch+json")]
    scalaSeq = converter.asScalaIteratorConverter(arrForScala.iterator()).asScala().toSeq();
    createIterationBodyStr = JsonOutput.toJson(body)
    def result = httpClient.azureClient.ws
        .url(baseUrl+"/${projectKey}/_apis/wit/workitems/${workItemId}?api-version=6.0")
        .addHttpHeaders(scalaSeq)
        .withAuth(token, token, play.api.libs.ws.WSAuthScheme$BASIC$.MODULE$)
        .withBody(play.api.libs.json.Json.parse(createIterationBodyStr), play.api.libs.ws.JsonBodyWritables$.MODULE$.writeableOf_JsValue)
        .withMethod("PATCH")
        .execute()
}

// This will get the PAT from the proxy user to do the extra API call
def getAccesToken(){
    def await = { f -> scala.concurrent.Await$.MODULE$.result(f, scala.concurrent.duration.Duration.apply(1, java.util.concurrent.TimeUnit.MINUTES)) }
    def creds = await(httpClient.azureClient.getCredentials())
    return creds.accessToken()
}

if(firstSync){
    // Set type name from source entity, if not found set a default
    workItem.projectKey  =  "Christophe" // Change this to your project key
   
    // Will first check the TypeMap if it does not find the type in the map it will search for exsisting issueType if it's still not found set it to a Task
    workItem.typeName = nodeHelper.getIssueType(replica.type?.name, (issue.projectKey ?: issue.project.key))?.name ?: "Task";
    workItem.summary      = replica.summary
    store(issue)
}

workItem.parentId = null
if (replica.parentId) {
  def localParent = syncHelper.getLocalIssueKeyFromRemoteId(replica.parentId.toLong())
  if (localParent)
    workItem.parentId = localParent.id
}

def baseUrl = syncHelper.getTrackerUrl()
def localUrl = ""
// link workItmes
def linkTypeMapping = [
    "blocks": "System.LinkTypes.Dependency-Reverse",
    "relates to": "System.LinkTypes.Related"
]
def jiraLinkedIssues = replica.linkedIssues
if (jiraLinkedIssues) {
    jiraLinkedIssues.each{
        def localParent = syncHelper.getLocalIssueKeyFromRemoteId(it.otherIssueId.toLong())
        if (!localParent?.id) { return; }
        localUrl = baseUrl + '/_apis/wit/workItems/' + localParent.id
        String mappedLinkType = linkTypeMapping[it.linkName] ?: "System.LinkTypes.Related"
        def body = [
            [
                op:"add",
                path:"/relations/-",
                value: [
                    rel:mappedLinkType,
                    url:localUrl,
                    attributes: [
                        comment:""
                    ]
                ]
            ]
        ]
        linkIssuesPatch(baseUrl, body, workItem.projectKey, getAccesToken(), workItem.key)
    }
}


// Remove issueLinks if they are not longer linked in Jira
def res = httpClient.get("/${workItem.projectKey}/_apis/wit/workitems/${workItem.key}?relations",true)
def adoLinkedIssues = res?.relations
List adoLinkedIssueKeys = []
List jiraLinkedIssueKeys = []

// make 2 lists ado and jira linked issueKeys
jiraLinkedIssues.each{
    int tmp = (int) it?.otherIssueId
    if(tmp != 0 && tmp != null)
        jiraLinkedIssueKeys.add(syncHelper.getLocalIssueKeyFromRemoteId(tmp.toString())?.id.toString())
}
adoLinkedIssues.each{
    adoLinkedIssueKeys.add(it.url.tokenize("/").last().toString())
}
List<Map> linkedIssues = adoLinkedIssues.collect { rel ->
    def issueKey = rel?.url.tokenize("/").last() // Extract issue key from URL
    [key: issueKey, url: rel?.url]
}

// Find items that need to be removed
List removeItems = adoLinkedIssueKeys - jiraLinkedIssueKeys
if(removeItems){
    // Collect the index of the issues link that needs to be removed
    List removeItemsIndex = removeItems.collect { adoLinkedIssueKeys.indexOf(it) }.findAll { it != -1 }

    // Prepare URLs to remove
    List removeUrls = removeItemsIndex.collect { adoLinkedIssues[it].url }
    
    // Iterate over URLs and perform the remove operation
    removeUrls.eachWithIndex { url, index ->

        def removeLinkBody = [
            [
                op: "remove",
                path: "/relations/${removeItemsIndex[index]}"
            ]
        ]
        linkIssuesPatch(baseUrl, removeLinkBody, workItem.projectKey, getAccesToken(), workItem.key)
    }
}