import groovy.json.JsonOutput


def setMessageSegment(String type){
  def mentionMatch = type =~ /\[~accountid:(.*?)]/
  if(mentionMatch.find()){
    // if no id was found add the email address as text not as mention.
    def userId = nodeHelper.getUserByEmail(mentionMatch.group(1))?.key
    if (!userId) return [type:"Text",text:mentionMatch.group(1)]
    return [type:"Mention",text:userId]  
  }
  return [type:"Text",text:type]    
}

def createSegmentBody(String comment){
  def regex = /\[~accountid:.*?]/
  // Use findAll to capture all user mentions
  def matches = comment.findAll(regex)
  def splitString = comment.split(regex)

  // Merge the split strings and matches
  def splittedComment = []
  for (int i = 0; i < splitString.size(); i++) {
    splittedComment << splitString[i]
    if (i < matches.size()) {
      splittedComment << matches[i]
    }
  }
  def segmentBody = []
    // iterate over the list and add the segment body
  for (int i = 0; i < splittedComment.size(); i++){
    if (splittedComment[i] == "") continue;
    if (i < splittedComment.size() - 1) {
      segmentBody.add(setMessageSegment(splittedComment[i]))
    }else{
      segmentBody.add(setMessageSegment(splittedComment[i]))
    }
  }
  // Return json object
  return [subjectId: entity.key, body: [ messageSegments: segmentBody]]
}

def addComments(def comments){
  def SFCommentId = ""
  // collect all comments and post the createSegmentBody...
  comments.collect{ c ->
    if (c.body){
      def res = httpClient.post("/services/data/v54.0/chatter/feed-elements/",JsonOutput.toJson(createSegmentBody(c.body)),null, ["Content-Type":["application/json"]]){
        req ,
        res ->
        // When the rest api call is successfully fulfilled set the SFCommentId var.
        if (res.code == 201) {
          SFCommentId = res?.body?.id
        } else {
          debug.error("error while creating comment:" + res.code + " message:" + res.body)
        }
      }
      // Add trace
      if ( SFCommentId ) {
        def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
            .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
            .setToSynchronize(true)
            .setLocalId(SFCommentId as String)
            .setRemoteId(c.remoteId as String)
            .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)
        traces.add(trace)
      }
    }
  }
}


if(firstSync){
  entity.entityType = "Case"
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)
  addComments(replica.addedComments)
}