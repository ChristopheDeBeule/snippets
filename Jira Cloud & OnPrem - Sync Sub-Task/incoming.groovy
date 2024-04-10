if(firstSync && replica.parentId){
    issue.typeName     = "Sub-task" //Make sure to use the right subtask type here.
	def localParent = nodeHelper.getLocalIssueFromRemoteId(replica.parentId.toLong())
	if(localParent){
		issue.parentId = localParent.id
	} else {
       throw new com.exalate.api.exception.IssueTrackerException("Subtask cannot be created: parent issue with remote id " + replica.parentId + " was not found. Please make sure the parent issue is synchronized before resolving this error" )
    }
}