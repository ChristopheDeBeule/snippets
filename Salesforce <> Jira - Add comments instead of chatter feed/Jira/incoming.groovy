def incomingSFcomment = replica.?caseComment?.records[0]?.CommentBody

if(incomingSFcomment){
    issue.comments     = commentHelper.addComment("${incomingSFcomment}", issue.comments)
    issue.comments = commentHelper.mergeComments(issue, replica,                     
                        {
                        comment ->
                        comment.body = incomingSFcomment
                            
                        }
    )
}
