// This will change the user mentions from an ID to an email (needed in ADO)
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