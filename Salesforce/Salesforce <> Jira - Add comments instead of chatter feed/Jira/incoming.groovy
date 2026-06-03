def incomingSFcomment = replica.caseComment
def existingBodies = issue.comments?.collect { it.body } ?: []

if(incomingSFcomment){
  if(!existingBodies.contains(incomingSFcomment)){
    issue.comments     = commentHelper.addComment("${incomingSFcomment}", false, issue.comments)
  }
}
