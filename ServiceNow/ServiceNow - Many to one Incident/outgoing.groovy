// From the remote side add a field where you cen get the incident number from
// In this use case we used a custom field (this field comes from jira cloud)
replica.customFields."Issue Key" = issue.customFields."Issue Key"


/*
Salesforce: replica."Issue Key" = entity."Issue Key"
Azure DevOps: replica.customFields."Issue Key" = workItem.customFields."Issue Key"
Zendesk: replica.customFields."Issue Key" = issue.customFields."Issue Key"
*/