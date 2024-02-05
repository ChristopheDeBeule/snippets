// Looks for the first char, the body of a comments first char is a '[' so we'll look at the second element in the list charAt(1)
// and before we search the first char we remove every leading whitespace.
char res = replica.addedComments.body.toString().replace(" ","").charAt(1)
// check which char it needs to find and then we'll remove the special char and '[',']' if there are any. We also set the comment internal
if(res == '+'){
    entity.comments = commentHelper.mergeComments(issue, replica, { comment -> 
        comment.body = replica.addedComments.body.toString().replaceAll("\\+","").replaceAll("\\[", "").replaceAll("\\]","")
        comment.internal = true
        }
    )
}else{
    entity.comments = replica.addedComments
}