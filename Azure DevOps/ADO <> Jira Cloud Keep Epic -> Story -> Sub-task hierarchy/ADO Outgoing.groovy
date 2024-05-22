replica.key            = workItem.key
replica.assignee       = workItem.assignee 
replica.summary        = workItem.summary
replica.description    = nodeHelper.stripHtml(workItem.description)
replica.type           = workItem.type
replica.status         = workItem.status
replica.labels         = workItem.labels
replica.priority       = workItem.priority
replica.comments       = nodeHelper.stripHtmlFromComments(workItem.comments)
replica.attachments    = workItem.attachments
replica.project        = workItem.project
replica.areaPath       = workItem.areaPath
replica.iterationPath  = workItem.iterationPath

replica.parentId = workItem.parentId
 
def res = httpClient.get("/_apis/wit/workitems/${workItem.key}?\$expand=relations&api-version=6.0",false)
if (res.relations != null)
    replica.relations = res.relations

//Send a Custom Field value
//replica.customFields."CF Name" = workItem.customFields."CF Name"