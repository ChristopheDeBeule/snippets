// This needs to be a custom field or a text field where you add the remote key in

// Examples:
// Jira Cloud, Jira OnPrem, Azure DevOps, Zendesk

replica.customFields."CF Name" = issue.customFields."CF Name"


// Salesforce, ServiceNow

replica."CF Name" = entity."CF Name"
