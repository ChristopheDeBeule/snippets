// match the .md format and replace it with the wiki format.
def convertMarkdownToJiraWiki(String text) {
    text = text.replaceAll(/\!\[(.*?)\]\((.*?)\)/, { match, altText, url -> "[$altText|$url]" })
    return text
}

// Call the function to replace md to wiki.
issue.comments = commentHelper.mergeComments(issue, replica, { c ->
    c.body = convertMarkdownToJiraWiki(c.body)
}) 