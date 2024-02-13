//linked issueKey
if(replica.linkedIssue && firstSync){
    issue.id = replica.linkedIssue?.id
    issue.key = replica.linkedIssue?.key
    return;
}

issue.summary      = replica.summary
issue.description  = replica.description
issue.comments     = commentHelper.mergeComments(issue, replica)
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.labels       = replica.labels
