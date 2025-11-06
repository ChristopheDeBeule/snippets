import groovy.json.JsonOutput

if(firstSync){
  entity.entityType = "Case"
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  entity.Origin       = "Web"
  entity.Status       = "New"
  //entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)


  replica.addedComments.collect {
 
  comment ->
   //  def internal = comment.internal ? false : true

  def SFCommentId = ""

  def res = httpClient.http(
      "POST",
      "/services/data/v54.0/sobjects/EmailMessage",
      // The subject and body can be changed to your likings
      JsonOutput.toJson([
          "Subject": "Re: Your support request",
          "TextBody": "[${comment.author.displayName}] commented : ${nodeHelper.stripHtml(comment.body)}",
          "ParentId" : "${entity.Id}", // Case ID or entity ID
          "FromAddress": "christophedb.private@gmail.com", // Send from
          "ToAddress": "christophe.debeule@exalate.com", // Send to
          "Incoming": false, // false = outgoing; true = inbound
          "Status": "3" // Set status ID
      ]),
      null,
      ['Content-Type': ['application/json']]
    ) {
      req ,
      res ->
      // When the rest api call is successfully fulfilled set the SFCommentId var.
      if (res.code == 201) {

          SFCommentId = res?.body?.id
      } else {
        throw new Exception("error while creating comment:" + res.code + " message:" + res.body)
      }
    }
    if ( SFCommentId ) {
      def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
              .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
              .setToSynchronize(true)
              .setLocalId(SFCommentId as String)
              .setRemoteId(comment.remoteId as String)
              .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)
      traces.add(trace)
    }
  }
}