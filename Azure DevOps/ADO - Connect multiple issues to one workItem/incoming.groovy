def remoteIssueUrn = replica.Demo__c // Salesforce customField (This can be any customField)
// removes whitespaces in string when you have a remote key 
if (remoteIssueUrn){
  remoteIssueUrn = remoteIssueUrn.replaceAll("\\s","")
}

if(remoteIssueUrn && firstSync){
  def localIssue = httpClient.get("/Christophe/_apis/wit/workitems/${remoteIssueUrn}",true).fields
  if(localIssue == null) throw new com.exalate.api.exception.IssueTrackerException("Issue with key "+remoteIssueUrn+" was not found")
  // set the id and Key of the workItem
  issue.id = localIssue?."System.Id"
  issue.key = localIssue?."System.Id"
  return;
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

