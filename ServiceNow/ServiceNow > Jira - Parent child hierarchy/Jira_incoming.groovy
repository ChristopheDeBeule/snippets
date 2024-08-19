String projectK = "DEMO"
if(firstSync && replica.parentId){
  issue.projectKey  = projectK
  issue.typeName     = "sub-task" //Make sure to use the right subtask type here.
	def localParent = nodeHelper.getLocalKeyFromRemoteId(replica.parentId, "incident")
	if(localParent){
		issue.parentId = localParent.id
	} else {
    throw new com.exalate.api.exception.IssueTrackerException("Subtask cannot be created: parent issue with remote id " + replica.parentId + " was not found. Please make sure the parent issue is synchronized before resolving this error" )
  }
}else if (firstSync) {
    issue.projectKey  = projectK
    // Set the same issue type as the source issue. If not found, set a default.
    issue.typeName    = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
}