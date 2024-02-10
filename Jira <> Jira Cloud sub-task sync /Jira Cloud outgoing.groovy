replica.parentId = issue.parentId

// Automatically sync all subtasks when syncing parent task
httpClient.get("/rest/api/3/issue/"+issueKey.id).fields.subtasks?.collect{
  def subTaskKey = new com.exalate.basic.domain.BasicIssueKey(it.id, it.key)
  syncHelper.exalate(subTaskKey)
}