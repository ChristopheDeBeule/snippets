import java.time.Instant
import java.util.Date

// Fetch the comments
def tmp = httpClient.get("/${workItem.projectKey}/_apis/wit/workItems/${workItem.key}/comments?api-version=7.1-preview.4".toString(), true)
def enrichedComments = null
if(tmp){
   tmp = tmp?.comments
   // Collect all comments and check the ID to change the right comment where needed
   enrichedComments = workItem.comments.collect{ hubComment ->
      def matchingAzureComment = tmp.find { it.id == hubComment.id }
      if(matchingAzureComment){
         // Here we can change the body, creation date, author etc with the values we get form the ADO comment.
         Date date = Date.from(Instant.parse(matchingAzureComment.createdDate))
         hubComment.body = "[Created on: ${date}]\n\n${hubComment.body}"
         hubComment.created = date
      }
      // Return the full Exalate comment object
      return hubComment
   }
}
// If there are no enriched comments just return the default setup.
if(enrichedComments){
   replica.comments = enrichedComments
}else{
   replica.comments = workItem.comments
}

workItem.comments = enrichedComments

throw new Exception("${replica.comments}")
