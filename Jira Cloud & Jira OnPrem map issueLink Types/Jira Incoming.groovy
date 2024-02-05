def linkMap = [
    "Fix":"Works"
]
replica.issueLinks.each{ l ->
    l.linkTypeName = linkMap[l.linkTypeName] ?: l.linkTypeName
}
issue.issueLinks = replica.issueLinks