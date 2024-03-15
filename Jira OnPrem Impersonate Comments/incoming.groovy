// map the key because Jira cloud does not send over the author email.
def userMap = [
    // "Jira Cloud user ID" :  "Jira OnPrem user email"
   "12a34bc5d6e7fg8h9012ij34":"johnDoe@exalate.com"
]
// When the user is not found in the map the nodeHelper will set the proxy user as the executor.
replica.addedComments.each { it.executor = nodeHelper.getUserByEmail(userMap[it.author?.key] ?: "") }
replica.changedComments.each { it.executor = nodeHelper.getUserByEmail(userMap[it.author?.key] ?: "") }

// There are 2 ways to set the author, just with {it} or you can change the body as well.
// 1: This will only set the author if exsist.
issue.comments = commentHelper.mergeComments(issue, replica, { it })

// 2: This will change the body of the comment as well.
issue.comments = commentHelper.mergeComments(issue, replica,                     
    {
        comment ->
        comment.body =
            "[" + comment.author.displayName +
                    "| email: " + comment.author.email + "]" +
                    " commented: \n" +
            comment.body + "\n"
    }
)
