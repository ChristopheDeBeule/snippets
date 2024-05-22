if(firstSync){
    issue.projectKey   = "DEMO"
   // Set type name from source issue, if not found set a default
   issue.typeName     = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
   issue.summary      = replica.summary
   store(issue) 
}


// We do a store(issue) so the issue.key will be created on the first sync and the request type will be set
// Note that customfield_10010 is the request type field and 33 is the request type id

def url = "/rest/api/3/issue/${issue.key}/"
def data = "{\"fields\": {\"customfield_10010\": \"33\"}}"
httpClient.put(url, data)
