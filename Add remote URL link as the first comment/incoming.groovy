// This will add the remote issue URL as the first comment if there are no comments yet or if the url is not yet in the comments

if(issue.comments.isEmpty() || !issue.comments.body.contains(issueUrl)){
  issue.comments +=  commentHelper.addComment(issueUrl, issue.comments)
}

// This can be different depending which connector you're using 

// Jira
issue.comments     = commentHelper.mergeComments(issue, replica)

// Zendesk 
issue.comments     += replica.addedComments


// etc..
