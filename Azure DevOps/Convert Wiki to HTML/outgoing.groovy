// Thiw will be the Jira cloud outgoing
// we need to format the userID to an email so we can check if the user exists on the ADO side.

replica.comments = issue.comments.collect{
  c ->
  def matcher = c.body =~ /\[~accountid:(.*?)]/ 
  matcher.each {
    def user = nodeHelper.getUser(matcher.group(1))?.email ?: "Stranger"         
    c.body = c.body.replace(matcher.group(1),user)
  }
  c 
}