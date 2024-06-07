def searchUrl = "/rest/api/3/search"
def remoteIssue = null
def parentKey = null

// Checks if there is a parentId
if(issue.parentId){
  def idJql = "id=${issue.parentId}".toString()
  // This will get the parent Key
  parentKey = httpClient.get(searchUrl, [jql:idJql])?.issues?.key.toString()
  // Removes '[]' from string
  if(parentKey){
    parentKey = parentKey.replaceAll(/\[|\]/, "")
  }
  // This will check if there is a remote parent for the subtaks.
  remoteIssue = nodeHelper.getLocalIssueFromRemoteId(issue.parentId.toLong())
}

// If there is a remoteIssue found and the issuetype is sub-task, then sync over the subtask
if (remoteIssue && issue.typeName == "sub-task"){
  httpClient.get("/rest/api/3/issue/${issue.key}").fields.subtasks?.collect{
    def subTaskKey = new com.exalate.basic.domain.BasicIssueKey(it.id, it.key)
    syncHelper.exalate(subTaskKey)
  }
}else if(parentKey){
  // This will sync the parent first and then the SubTask
  // Create parent first
  def subParent = new com.exalate.basic.domain.BasicIssueKey(issue.parentId, parentKey)
  syncHelper.exalate(subParent)
}
