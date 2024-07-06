// Change the mention id to the email
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