import java.time.Instant
import java.util.Date

def tmp = httpClient.get("/${workItem.projectKey}/_apis/wit/workItems/652/comments?api-version=7.1-preview.4".toString(), true)
def enrichedComments = null
if(tmp){
   tmp = tmp?.comments
   enrichedComments = workItem.comments.collect{ hubComment ->
      def matchingAzureComment = tmp.find { it.id == hubComment.id }
      if(matchingAzureComment){
         hubComment.created = Date.from(Instant.parse(matchingAzureComment.createdDate))
      }
      return hubComment
   }
}
if(enrichedComments){
    replica.comments = enrichedComments
}else{
    replica.comments = workItem.comments
}

throw new Exception("${replica.comments}")
