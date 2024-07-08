// Change the mention id to the email 

// Jira Cloud outgoing
def changeString(String comment){
  def matcher = comment =~ /\[~accountid:(.*?)]/ 
  if (!matcher.find()) return comment
  
  matcher.each{
    comment = comment.replace(matcher.group(1), nodeHelper.getUser(matcher.group(1))?.email ?: "Stranger")
  }
  return comment
}
replica.comments = issue.comments.collect{
  c ->
  c.body = changeString(c.body)
  c 
}


// Azure DevOps outgoing
def changeString(String comment){
  def matcher = comment =~ /<a href="#" data-vss-mention="version:[0-9.]+,([a-f0-9-]+)">[^<]+<\/a>/
  if (!matcher.find()) return comment
  matcher.each{
  def userEmail = nodeHelper.getUser(matcher.group(1), workItem.projectKey)?.email ?: "johnDoe@tmp.com"
  comment = comment.replace(matcher.group(0), "[~accountid:${userEmail}]")
  }
  return comment
}
replica.comments = issue.comments.collect{
  c ->
  c.body = changeString(c.body)
  c 
}