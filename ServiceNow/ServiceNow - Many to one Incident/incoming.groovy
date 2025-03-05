def remoteIssue = replica.customFields."Issue Key"?.value
// check if the remote issue needs to create a new incident or connect to one
if(firstSync && remoteIssue){
  remoteIssue = remoteIssue.trim()
  // check if the given incident number exists or not
  def localissue = nodeHelper.getReference("incident", "number", remoteIssue)
  if(localissue){
    // Create a new issue key from the existing incident number
    def issueKey = new com.exalate.basic.domain.BasicIssueKey(localissue.sys_id,localissue.number,"incident")
    entity.id = issueKey.idStr
    return new scala.Tuple2(issueKey, scala.collection.JavaConverters.asScalaBuffer(traces).toSeq())
  }
}else if (firstSync) {
  // For the first sync: Decide the entity you want to create based on the remote issue type
  entity.tableName = "incident"
}

if (entity.tableName == "incident") {
    entity.short_description  = replica.summary
    entity.description        = replica.description
    entity.attachments        += replica.addedAttachments
    entity.comments           += replica.addedComments
}