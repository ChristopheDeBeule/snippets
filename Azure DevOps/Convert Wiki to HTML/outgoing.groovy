// This will change the user mentions from an ID to an email (needed in ADO) (Jira cloud outgoing)
replica.comments = issue.comments.collect{
  c ->
  def matcher = c.body =~ /\[~accountid:(.*?)]/ 
  matcher.each {
    def user = nodeHelper.getUser(matcher.group(1)) ?: "john@doe.com" 
    def newBodyreplacement = "${user?.email}|${user?.displayName}"     
    c.body = c.body.replace(matcher.group(1),newBodyreplacement)
  }
  c
  
}

// This will change the user mentions from an ID to an email (needed in ADO) (Description & custom rich text fields)
def matcherDescription = issue.description =~ /\[~accountid:(.*?)]/ 
matcherDescription.each {
  def user = nodeHelper.getUser(matcherDescription.group(1)) ?: "john@doe.com" 
  def newBodyreplacement = "${user?.email}|${user?.displayName}"     
  issue.description = issue.description.replace(matcherDescription.group(1),newBodyreplacement)
}

replica.description    = issue.description