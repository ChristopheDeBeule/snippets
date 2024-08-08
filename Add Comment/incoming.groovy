String customTextField = replica.customFields."Demo"?.value

// When we have a custom field value and it does not exist in the comments add the cf value to the comment as a public one.
if (customTextField && !issue.comments.collect{it.body.contains(customTextField)}.contains(true)){
  // Add the comment to the comment list
  issue.comments = commentHelper.addComment(customTextField, false, issue.comments).collect{
    if (it.body.contains(customTextField)){
      it.internal = false
      it.body = "Custom Field Comment: ${it.body}"
    }
    it
  }
}