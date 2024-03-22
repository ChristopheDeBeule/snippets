if(firstSync){
   // Set type name from source entity, if not found set a default
   workItem.projectKey  =  "Christophe"
   workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
}

workItem.summary      = replica.summary
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority
workItem.description = replica.description


def setInlineImage(def comment){
    // List of possible regex paterns
    def patternList = [
        /<p><!-- inline image filename=#(.*?)#<\/a> --><\/p>/,
        /(?s)<!-- inline image filename=(.*?)-->/ ,
        /<p>&lt;!-- inline image filename=#(.*?)#<\/a> --&gt;<\/p>/,
        /filename=#(.+?)# --&gt;/
    ]

    def pattern = null
    def matcher = null
    patternList.each{ p ->
        matcher = comment =~ p
        if(matcher.find()){
            matcher = comment =~ p
        }
    }

    def inlineImage = ""

    if (matcher.find()) {
        def match = matcher[0]
        def attId = replica.attachments.find { it.filename?.equals(match[1]) }?.remoteId
        inlineImage = comment.replace(match[0], """<img src="/secure/attachment/${attId}/${attId}_${match[1]}" />""".toString())
    }else{
        return comment // return the comment if matcher is null
    }
    // Sometimes the string still contains "<!-- inline image" This part will remove that
    inlineImage = inlineImage.replaceAll("<!-- inline image", "").replaceAll("&lt;!-- inline image", "")
    return inlineImage
}

workItem.comments     = commentHelper.mergeComments(workItem, replica, {
    comment ->
    comment.body = setInlineImage(comment.body) 
})