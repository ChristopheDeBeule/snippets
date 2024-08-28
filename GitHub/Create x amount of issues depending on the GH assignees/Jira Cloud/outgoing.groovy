replica.key            = issue.key
replica.type           = issue.type
replica.assignee       = issue.assignee
replica.reporter       = issue.reporter
replica.summary        = issue.summary
replica.description    = issue.description
replica.labels         = issue.labels
replica.comments       = issue.comments
replica.resolution     = issue.resolution
replica.status         = issue.status
replica.parentId       = issue.parentId
replica.priority       = issue.priority
replica.attachments    = issue.attachments
replica.project        = issue.project



// Custom field to store all created issues in
def createdIssuesValues = issue.customFields."Rich Text Field"?.value
if(createdIssuesValues){
    // Remove unused '[', ']'
  createdIssuesValues = createdIssuesValues.replaceAll(/\s*,\s*self:https:\/\/[^,\]}]+\s*(,|\})?/, '').replaceAll(/\]\[/, ',')
  replica.createdIssuesValues = createdIssuesValues
}
