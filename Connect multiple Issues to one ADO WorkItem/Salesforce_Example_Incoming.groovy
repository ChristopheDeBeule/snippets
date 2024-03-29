// This will show you the connected issues in the description (The Case ID linked to the SF case)

def remoteIssueUrn = replica.Demo__c
// removes whitespaces in string 
if (remoteIssueUrn){
  remoteIssueUrn = remoteIssueUrn.replaceAll("\\s","")
}

def findCaseIDFromSfUrl(String url){
   // Regex to match the Case ID pattern
  def pattern = ~/Case\/(.*?)\/view/
  def matcher = url =~ pattern

  if (matcher.find()){
    return matcher.group(1)
  }else {
    return "Case ID not found"
  }
}

if(remoteIssueUrn && firstSync){
  def localIssue = httpClient.get("/Christophe/_apis/wit/workitems/${remoteIssueUrn}",true).fields
  if(localIssue == null) throw new com.exalate.api.exception.IssueTrackerException("Issue with key "+remoteIssueUrn+" was not found")
  
  issue.id = localIssue?."System.Id"
  issue.key = localIssue?."System.Id"

  workItem.description  += "<a href=\"${issueUrl}\">${findCaseIDFromSfUrl(issueUrl)}</a>"

  return;
}

if(firstSync){
  // Set type name from source entity, if not found set a default
  workItem.projectKey  =  "Christophe"
  workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
  workItem.summary      = replica.summary
}


//workItem.description  = replica.description
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority
