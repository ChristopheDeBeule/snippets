def remoteIssueUrn = replica.customFields."CF name"?.vale
// This will remove posible spaces, cannot do a replaceAll on null object that's why we de the if statement. 
if (remoteIssueUrn){
  remoteIssueUrn = remoteIssueUrn.replaceAll("\\s","")
}


// This will check if it's the first sync and we do have a issueKey to link the remote issue to
if(remoteIssueUrn && firstSync){
    // Connect the remote issue to the given issue in Jira
    def localIssue = httpClient.get("/rest/api/2/issue/"+remoteIssueUrn)
    if(localIssue == null) throw new com.exalate.api.exception.IssueTrackerException("Issue with key "+remoteIssueUrn+" was not found")
    issue.id = localIssue?.id
    issue.key = localIssue?.key

    // set counter in custom rich text field and add link to custom field

    // First we get the custom field via the rest api so we can see if it already has values
    def customRichTextField =  httpClient.get("/rest/api/3/issue/${issue.key}")?.fields?.customfield_10442
    def foundNumber = customRichTextField.content.size() // content is empty if the field has no value
    
    // We keep track off the linked issues via the count variable, we increase it everytime a issue is added.
    def count = 0
    // check if the field already has a value, if not we set the customfield "CF Name" value to a prefered string.
    // In this case we add the String "Connected Ticket: [${replica.summary}|${issueUrl}]\n" and we increment the count variable.
    if (foundNumber <= 0){
      issue.customFields."CF Name".value = "Connected Ticket: [${replica.summary}|${issueUrl}]\n" 
      count++
    }else{
      // If you add values to the customRichTextField field it will show null first, this is why we loop over the field and add it like this.
      // This will empty the field and add the new link into it (This prevents duplicates), but we get all the values back via the rest api.
      issue.customFields."CF Name".value = ""
      
      // tmp will store the right values we need.
      def tmp = ""
      
      // This loop will set the values back to the ones we already had but count the lines with it so we always have the right count.
      customRichTextField.content.content[0].each{ item ->
        if (!(item.text?.contains("Connected Ticket") || item.type == "hardBreak")){
          tmp = "[${item.text}|${item.marks[0].attrs.href}]"
          issue.customFields."CF Name".value += "Connected Ticket: ${tmp}\n"
          count++
        }
      }
      // Here we add the newly conected issue and increase the count variable
      issue.customFields."CF Name".value += "Connected Ticket: [${replica.summary}|${issueUrl}]\n"
      count++
    }

    // In this custom field we keep the count.
    issue.customFields."Total Salesforce Cases".value = count
  return;
}