def remoteIssueUrn = replica.customFields."Demo"?.value

if (firstSync){
    issue.summary      = replica.summary
    // Remove leading and ending whitespace
    if(remoteIssueUrn){
        remoteIssueUrn = remoteIssueUrn.trim()
    }
    // Make sure the ticket exists on this ZD instance.
    def localTicket = httpClient.get("/api/v2/tickets/${remoteIssueUrn}")
    if (localTicket){
        issue.id = remoteIssueUrn
        issue.key = remoteIssueUrn
    }
}

issue.labels       = replica.labels
issue.description  = replica.description ?: "No description"
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.comments     += replica.addedComments
