// Fetch and create a simple ID -> ParentId map (This prevents having nested loops or collects later on)
def parentMap = httpClient.get("/rest/api/3/issue/${issue.key}/comment")?.comments?.collectEntries {
  [it.id.toString(), it.parentId?.toBigInteger()?.toString()]
} ?: [:]

// Update the local comments using the lookup map
def comments = issue.comments.collect { comment ->
  if (comment.role == null) {
    // Assign parentId from map if found in the mapping, using role to store this value for now as a workaround
    comment.role = parentMap[comment.idStr]
  }
  comment
}

replica.comments = comments