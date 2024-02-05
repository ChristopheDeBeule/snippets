if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   def typeMap = [
      // "ADO side":"Jira Cloud side"
      "Feature":"Epic",
      "Epic":"Feature",
      "User Story":"Story"
   ]
   issue.typeName     = nodeHelper.getIssueType(typeMap[replica.type?.name], issue.projectKey)?.name ?: nodeHelper.getIssueType(replica.type?.name, (issue.projectKey ?: issue.project.key))?.name ?: "Task"
   issue.summary      = replica.summary
   if (replica.typeName == "Feature") {
      issue.customFields."Epic Name".value = replica.summary
   }
   store(issue)
}
// This will check if the issueType is a Feature and if there is a parentId now it will link the epic with the feature.
// And it will only link the issues as a child issue under the feature
if (issue.typeName == "Feature" && replica.parentId) {
    def localParent = syncHelper.getLocalIssueKeyFromRemoteId(replica.parentId.toLong())
    if (localParent) {
        issue.customFields."Epic Link".value = localParent.urn
    }
}else {

   replica.relations.each {   
      relation ->
      // We check on the Related attribute from ADO and link it wiht Relates in Jira
         if (relation.attributes.name == "Parent"){
            def a = syncHelper.getLocalIssueKeyFromRemoteId(relation.url.tokenize('/')[7])//?.urn   
            if (issue.issueLinks[0]?.otherIssueId != a?.id){
                def res = httpClient.put("/rest/api/2/issue/${issue.key}", """
                {
                   "update":{
                      "issuelinks":[
                         {
                            "add":{
                               "type":{
                                  "name":"Relates"
                               },
                               "outwardIssue":{
                                  "key":"${a.urn}"
                               }
                            }
                         }
                      ]
                   }
                }
                """)
            }
        }
   }
}

issue.summary      = replica.summary
issue.description  = replica.description
issue.comments     = commentHelper.mergeComments(issue, replica)
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.labels       = replica.labels