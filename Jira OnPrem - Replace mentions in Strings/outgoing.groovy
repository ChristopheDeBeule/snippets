// This wil replace mentions in a string
// mentions: [~ jiraUserName]

def replaceMentions(String str) 
    // This patern will search for all the [~mentions]
    def pattern = /\[\~[^\]]+\]/
    // Find all matches in the string
    def matches = (str =~ pattern)
    // Use this patern to find the username
    def replacePattern = /\[~(.+)\]/ 

    // iterate over every match it found
    matches.each { c ->
        // use this matcher to find the display name
        matcher = (c =~ replacePattern)
        username = matcher[0][1]
        // If we have a display name we replace it
        if (username){
            target = nodeHelper.getUserByUsername(username)?.displayName
            str = str.replace(c, target)
        }
    }
    return str
}

replica.description = replaceMentions(issue.description)

replica.comments = issue.comments.collect { comment ->
    comment.body = replaceMentions(comment.body)
    comment
}
