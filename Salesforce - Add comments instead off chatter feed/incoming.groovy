import groovy.json.JsonOutput


// Collect added comments.
replica.addedComments.collect {
 
    comment ->
    def internal = "true"
    if (comment.internal) {
        internal = "false"
    }
    def SFCommentId = ""

    // Add the comment as a case comment istead of chatter feed.
    def res2 = httpClient.http(
        "POST",
        "/services/data/v54.0/sobjects/CaseComment",
        JsonOutput.toJson([
            "CommentBody": "[${comment.author.displayName}] commented : ${nodeHelper.stripHtml(comment.body)}",
            "ParentId" : "${entity.Id}", 
            "isPublished": "${internal}" 
        ]),
        null,
        null
    ) {
        req ,
        res2 ->
        // When the rest api call is successfully fulfilled set the SFCommentId var.
        if (res2.code == 201) {

            SFCommentId = res2?.body?.id
        } else {
            debug.error("error while creating comment:" + res2.code + " message:" + res2.body)
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
