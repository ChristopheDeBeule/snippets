String localCaseRecord = replica.customFields."Demo"?.value
if(firstSync && localCaseRecord){
  localCaseRecord = localCaseRecord.trim()
  def res = httpClient.get("/services/data/v60.0/sobjects/Case/${localCaseRecord}")
  if(res){    
    def issueKey = new com.exalate.basic.domain.BasicIssueKey(res?.Id, res?.Id,"Case")
    entity.id = issueKey.idStr
    return new scala.Tuple2(issueKey, scala.collection.JavaConverters.asScalaBuffer(traces).toSeq())
  }
}else if(firstSync){
  entity.entityType = "Case"
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)
}
