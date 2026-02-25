import groovy.json.JsonOutput 

Map mention(String user) {
  if(!user) return null
  Map userMen = [
    type: "mention",
    attrs: [
      id: user,       
      text: "@"
    ]
  ]
}

Map textToAdf(String text, String userId = null) {
  def contentList = []

  def userMention = mention(userId)
  if (userMention) contentList << userMention

  contentList << [type: "text", text: text]

  return [
    type: "doc",
    version: 1,
    content: [
      [
        type: "paragraph",
        content: contentList
      ]
    ]
  ]
}


Map parentOldIdToNewId = [:]
String commentId = null
// Check on role and remote ID, Create the comment and check does this comment have a parent (role == null) store the new created comment ID
// When we have this ID we can set the child comments where remoteId == role  
replica.addedComments.each{ comment -> 
  // first create all the parent comments
  if(comment.role == null){
    Map body = [
      "body": textToAdf("${comment.body}".toString()),
      "public": "${!comment.internal}",
    ]

    def tmpRes = httpClient.post("/rest/api/3/issue/${issue.key}/comment", JsonOutput.toJson(body))

    parentOldIdToNewId.put(comment.remoteId.toString() ,tmpRes.id)
    
    commentId = tmpRes.id
    if(commentId){
      def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
        .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
        .setToSynchronize(true)
        .setLocalId(commentId as String)
        .setRemoteId(comment.remoteId as String)
        .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)
      traces.add(trace)
    }
  }
}

replica.addedComments.each{ comment -> 
  if(comment.role != null){
    def matcher = comment.body =~ /(?<=\[~accountid:)([a-zA-Z0-9]+)(?=\])/
    def firstAccountId = matcher.find() ? matcher.group(0) : null
    def parentId = parentOldIdToNewId[comment.role] ?: traces.find { it.remoteId == comment.role}?.localId
    if(firstAccountId){
      comment.body = comment.body.replaceAll(/\[~accountid:[a-zA-Z0-9]+\]\s*/, " ")
    }
    Map body = [
      "body": textToAdf("${comment.body}".toString(), firstAccountId),
      "public": "${!comment.internal}",
      "parentId": "${parentId}"
    ]

    def tmpRes = httpClient.post("/rest/api/3/issue/${issue.key}/comment", JsonOutput.toJson(body))

    commentId = tmpRes.id
    if(commentId){
      def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
        .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
        .setToSynchronize(true)
        .setLocalId(commentId as String)
        .setRemoteId(comment.remoteId as String)
        .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)
      traces.add(trace)
    }
  }
}
