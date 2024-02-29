// Checks if a comment contains #zd if not do nothing
def comment = workItem.comments
Boolean containsZD = comment.body[0].toLowerCase()?.contains("#zd")
if (!containsZD) return
// Only add the last comment when the remote site uses += replica.addedComments you can comment this out
// But when the remote side has commentHelper.mergeComments(issue, replica)
// You'll need to use this otherwise it will add all commens even the ones without #zd.
workItem.comments = [workItem.comments[0]] 

replica.key            = workItem.key
replica.assignee       = workItem.assignee 
replica.summary        = workItem.summary
replica.description    = nodeHelper.stripHtml(workItem.description)
replica.type           = workItem.type
replica.status         = workItem.status
replica.labels         = workItem.labels
replica.priority       = workItem.priority
replica.comments       = nodeHelper.stripHtmlFromComments(workItem.comments)
replica.attachments    = workItem.attachments
replica.project        = workItem.project
replica.areaPath       = workItem.areaPath
replica.iterationPath  = workItem.iterationPath
