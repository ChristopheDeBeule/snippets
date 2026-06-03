def caseComment = httpClient.get("/services/data/v56.0/query/?q=SELECT Id, CommentBody FROM CaseComment WHERE ParentId ='${entity.Id}' ORDER BY CreatedDate DESC LIMIT 1")?.records?.CommentBody
if(caseComment){
    replica.caseComment = caseComment[0]
}