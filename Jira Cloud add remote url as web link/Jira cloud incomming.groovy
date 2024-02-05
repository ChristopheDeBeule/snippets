if(firstSync){
    issue.projectKey   = "DEMO" 
    issue.typeName     = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
    issue.summary      = replica.summary
    store(issue) 
    // Only add the webLink on the first sync otherwise you'll get duplicates
    def url = "/rest/api/3/issue/${issue.key}/remotelink"
    def data = "{\"object\": {\"url\":\"${issueUrl}\",\"title\": \"Remote Issue Link '${replica.key}'\"}}"
    httpClient.post(url, data)
}