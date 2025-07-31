if (firstSync) {
  // Set the project and workItem type
  issue.projectKey  = "TEAM"
  issue.type        = nodeHelper.getIssueType(replica.type?.name, issue.projectKey) ?: nodeHelper.getIssueType("Task", issue.projectKey)
  // Checks if the remote issue has a parent
  if (replica.parentId) {
    def localParent = nodeHelper.getLocalIssueFromRemoteId(replica.parentId.toLong())
    // checks if the parent is already synced in this instance
    if (localParent) {
      issue.parentId = localParent.id
    }
    // If the type is "sub-task", we directly assign the type name
    if (replica.type?.name == "sub-task") {
      // Overwrite the issue type if its a subtask on the remote end.
      issue.typeName = "Subtask"  // Ensure to use the right subtask type here
    }
  }
}
