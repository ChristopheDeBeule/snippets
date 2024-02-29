/// Strip HTML form comments SF Jira incoming
def stripHtml(String html) {
    return html.replaceAll(/<[^>]*>/, '')
}

issue.comments     = commentHelper.mergeComments(issue, replica, {
    comment ->
    comment.body = stripHtml(comment.body)
})