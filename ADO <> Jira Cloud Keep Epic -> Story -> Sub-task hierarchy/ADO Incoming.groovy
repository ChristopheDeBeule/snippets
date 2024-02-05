if(firstSync){
    // Set type name from source entity, if not found set a default
    workItem.projectKey  =  "Christophe"
    def typeMap = [
      // "Jira Cloud":"ADO"
      "Story" : "User Story"
   ]
    // Will first check the TypeMap if it does not find the type in the map it will search for exsisting issueType if it's still not found set it to a Task
     workItem.typeName = typeMap[replica.type?.name] ?: nodeHelper.getIssueType(replica.type?.name, (issue.projectKey ?: issue.project.key))?.name ?: "Task";
    workItem.summary      = replica.summary
    store(issue)
}

if (replica.parentId) {
    def localParent = syncHelper.getLocalIssueKeyFromRemoteId(replica.parentId.toLong())
    if (localParent) {
      workItem.parentId = localParent.id
    }
}

def res =httpClient.get("/Christophe/_apis/wit/workItems/${workItem.id}/revisions",true)
def await = { f -> scala.concurrent.Await$.MODULE$.result(f, scala.concurrent.duration.Duration.apply(1, java.util.concurrent.TimeUnit.MINUTES)) }
def creds = await(httpClient.azureClient.getCredentials())
def token = creds.accessToken()
def baseUrl = creds.issueTrackerUrl()
def project = workItem.projectKey
def localUrl = baseUrl + '/_apis/wit/workItems/' + workItem.id
int x =0
res.value.relations.each{
    revision ->
        def createIterationBody1 = [
            [
                op: "test",
                path: "/rev",
                value: (int) res.value.size()
            ],
            [
                op:"remove",
                path:"/relations/${++x}"
            ]
        ]
 
}
 
def linkTypeMapping = [
    "relates to": "System.LinkTypes.Related"
]
def linkedIssues = replica.linkedIssues
if (linkedIssues) {
    replica.linkedIssues.each{
       def localParent = syncHelper.getLocalIssueKeyFromRemoteId(it.otherIssueId.toLong())
       if (!localParent?.id) { return; }
       localUrl = baseUrl + '/_apis/wit/workItems/' + localParent.id
     def createIterationBody = [
            [
                op: "test",
                path: "/rev",
                value: (int) res.value.size()
            ],
            [
                op:"add",
                path:"/relations/-",
                value: [
                    rel:linkTypeMapping[it.linkName],
                    url:localUrl,
                    attributes: [
                        comment:""
                    ]
                ]
            ]
        ]
 
    def createIterationBodyStr = groovy.json.JsonOutput.toJson(createIterationBody)
        converter = scala.collection.JavaConverters;
        arrForScala = [new scala.Tuple2("Content-Type","application/json-patch+json")]
        scalaSeq = converter.asScalaIteratorConverter(arrForScala.iterator()).asScala().toSeq();
        createIterationBodyStr = groovy.json.JsonOutput.toJson(createIterationBody)
        def result = await(httpClient.azureClient.ws
            .url(baseUrl+"/${project}/_apis/wit/workitems/${workItem.id}?api-version=6.0")
            .addHttpHeaders(scalaSeq)
            .withAuth(token, token, play.api.libs.ws.WSAuthScheme$BASIC$.MODULE$)
            .withBody(play.api.libs.json.Json.parse(createIterationBodyStr), play.api.libs.ws.JsonBodyWritables$.MODULE$.writeableOf_JsValue)
            .withMethod("PATCH")
            .execute())
    
    }
}

// This set the status depending on which issueType (Change this to the ones you need)
def setStatus(){
   def statusMappingEpic = [
        "Open":"Open", 
        "In Progress":"Doing", 
        "Done":"Closed"
    ]
    def statusMappingFeature = [
        "Open":"To Do", 
        "In Progress":"Doing", 
        "Done":"Closed"
    ]
     def statusMappingStory = [
        "Open":"To Do", 
        "In Progress":"Doing", 
        "Done":"Closed"
    ]
    def remoteStatusName = replica.status.name
    if (issue.type.name == "Epic"){ return statusMappingEpic[remoteStatusName] ?: "Open"}
    if (issue.type.name == "Feature"){ return statusMappingFeature[remoteStatusName] ?: "To Do"}
    if (issue.type.name == "User Story"){ return statusMappingStory[remoteStatusName] ?: "To Do"}
}
if (!firstSync){
    workItem.setStatus(setStatus())
}

workItem.summary      = replica.summary
workItem.description  = replica.description
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority

