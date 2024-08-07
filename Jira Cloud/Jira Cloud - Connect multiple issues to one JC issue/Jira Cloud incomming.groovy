def remoteIssueUrn = replica.customFields."CF Name"?.value 
// This will check if there is a remote issue URN so we can do a replace otherwise you do a replace on a null object which can cause errors.
if (remoteIssueUrn){
    remoteIssueUrn = remoteIssueUrn.trim()
}
// removes whitespaces in string 
if(remoteIssueUrn && firstSync){
    def localIssue = httpClient.get("/rest/api/2/issue/"+remoteIssueUrn)
    if(localIssue == null) throw new com.exalate.api.exception.IssueTrackerException("Issue with key "+remoteIssueUrn+" was not found")
    issue.id = localIssue?.id
    issue.key = localIssue?.key
    
    // set counter in custom text field
    def getCustomFieldValue = httpClient.get("/rest/api/3/issue/${issue.key}")?.fields?.customfield_10035 // String NOTE: customfield_10035 is the custom field where you want to set the counter in (This is a text field)
    def foundNumber = 0
    if (getCustomFieldValue){
        // This will check if there is a number in your string and use that number
        foundNumber = (getCustomFieldValue =~ /\d+/)[0]
    }
    issue.customFields."Demo".value = "Connected Tickets: ${(foundNumber.toInteger() + 1).toString()}"
    return;
}
