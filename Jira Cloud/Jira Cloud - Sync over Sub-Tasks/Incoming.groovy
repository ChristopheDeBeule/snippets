if(firstSync && replica.parentId && replica.type?.name == "sub-task"){
  issue.typeName     = "sub-task"
  issue.projectKey   = "Your project Key"
	def localParent = nodeHelper.getLocalIssueFromRemoteId(replica.parentId.toLong())
	if(localParent){
		issue.parentId = localParent.id
	}else {
    throw new com.exalate.api.exception.IssueTrackerException("Subtask cannot be created: parent issue with remote id " + replica.parentId + " was not found. Please make sure the parent issue is synchronized before resolving this error" )
  }
}