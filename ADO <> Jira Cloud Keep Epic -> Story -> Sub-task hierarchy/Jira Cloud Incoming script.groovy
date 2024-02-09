// This will set the new Parent field in Jira Cloud
def setNewParentField(def localIssue){
   def url = "/rest/api/3/issue/${issue.key}/"
   def dataKey = null
   // We'll check if the remote side has an Epic Link, if they removed their epic link it will also be removed on the local side.
   // If we want to keep the epic link even when removed on the remote site remove the null check and the dataKey var.
   if(localIssue != null) dataKey = "\"${localIssue}\""

   def data = "{\"fields\": {\"parent\": {\"key\" : ${dataKey}}}}"
   
   httpClient.put(url,data)
}

if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   def typeMap = [
      // "ADO side":"Jira Cloud side"
      "Epic":"Epic",
      "Feature":"Feature",
      "User Story":"Story"
   ]
   issue.typeName     = nodeHelper.getIssueType(typeMap[replica.type?.name], issue.projectKey)?.name ?: nodeHelper.getIssueType(replica.type?.name, (issue.projectKey ?: issue.project.key))?.name ?: "Task"
   issue.summary      = replica.summary
   store(issue)

}
// This will check if the issueType is a Feature and if there is a parentId now it will link the epic with the feature.
// And it will only link the issues as a child issue under the feature
// NOTE: Change the issueType to the type that needs the parent link.
if (issue.typeName == "Feature" && replica.parentId) {
   def localParent = syncHelper.getLocalIssueKeyFromRemoteId(replica.parentId.toLong())
   setNewParentField(localParent.urn)
   
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
