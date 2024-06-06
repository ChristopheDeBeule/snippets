// OLD way
// def setNewParentField(){
//    // This will set the new Parent field in Jira Cloud
//     def remoteEpic = replica.customFields."Epic Link"?.value?.urn
//     def localIssue = nodeHelper.getLocalIssueFromRemoteUrn(remoteEpic)?.key
//     def url = "/rest/api/3/issue/${issue.key}/"
//     def dataKey = null
//     // We'll check if the remote side has a Epic Link, if they removed their epic link it will also be removed on the local side.
//     // If we want to keep the epic link even when removed on the remote site remove the null check and the dataKey var.
//     if(remoteEpic != null) dataKey = "\"${localIssue}\""

//     def data = "{\"fields\": {\"parent\": {\"key\" : ${dataKey}}}}"
    
//     httpClient.put(url,data)
// }

// NEW Way
def setNewParentField(remoteIssue){
    // Make sure the issue Type is not a sub-task and that we have a parentId
  if(!replica.parentId || replica.type?.name == "sub-task") return
  def localIssue = nodeHelper.getLocalIssueFromRemoteId(replica.parentId.toLong())
  if(localIssue){
    issue.parentId = localIssue.id
  }
}

if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   issue.typeName     = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
   issue.summary      = replica.summary
   store(issue) 
   setNewParentField()
}
// If the issue is already synced over and you add the Epic Link then it will be set or if you change the Epic link
if (!firstSync){
    setNewParentField()
}