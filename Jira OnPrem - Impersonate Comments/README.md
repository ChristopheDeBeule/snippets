# Set comment executor in Jira onPrem


This will impersonate and set the comment executor in Jira DC & Server

Use this if you don't get the comment author email, you can use the Jira cloud ID (key) to map it to the right user.

This is a workaround.


#### Remote outgoing script

```
replica.comments = issue.comments
```


#### jira OnPrem Incomming script


Here we define a user map that will map the Jira cloud ID to the user email from your onPrem side.

```
def userMap = [
    // "Jira Cloud user ID" :  "Jira OnPrem user email"
   "12a34bc5d6e7fg8h9012ij34":"johnDoe@exalate.com"
]

replica.addedComments.each { it.executor = nodeHelper.getUserByEmail(userMap[it.author?.key] ?: "") }
replica.changedComments.each { it.executor = nodeHelper.getUserByEmail(userMap[it.author?.key] ?: "") }
issue.comments = commentHelper.mergeComments(issue, replica, { it })
```


You can use the following script example:

[Script](./incoming.groovy)


**Referrals:**

[How to Impersonate a Comment in Jira Cloud](https://docs.exalate.com/docs/how-to-impersonate-a-comment-in-jira-cloud)

[How to Impersonate a Comment in Jira On-premise](https://docs.exalate.com/docs/how-to-impersonate-a-comment-in-jira-on-premise)
