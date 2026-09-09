def remoteIssueUrn = replica.Demo__c // Salesforce customField (This can be any customField)

if(remoteIssueUrn && firstSync){
  remoteIssueUrn = remoteIssueUrn.trim()
  def localIssue = httpClient.get("/${projectName}/_apis/wit/workitems/${remoteIssueUrn}",true)?.fields
  if(localIssue == null) throw new Exception("WorkItem Not Found.")
  int systemId = localIssue?."System.Id" instanceof Double ? localIssue?."System.Id".toInteger() : localIssue?."System.Id"
  issue.id  = systemId
  issue.key = systemId
  return 
}
// Only on the first sync (workItem creation) it will set the summary
// If you set the summary out of the first sync it will be overwritten with the last issue that is connected
if(firstSync){
  // Set type name from source entity, if not found set a default
  workItem.projectKey  =  "Christophe"
  workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
  workItem.summary      = replica.summary
}


workItem.description  = replica.description
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority

