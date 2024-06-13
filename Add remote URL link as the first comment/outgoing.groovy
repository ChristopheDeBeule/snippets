// This will filter out the Jira (atlassian) comments so the Jira issue url will not be added in the remote comments


// NOTE: this will check the url so we check first does it start with http and then does it contain the local domain (the domain is easy to look for in a url)
// Change the domain ("atlassian") to your local domain -> Zendesk, serviceNow, Azure DevOps, Salesfroce, GitHub... (Jira on Prem use your own domain)
replica.comments = issue.comments.collect{ it.body.startsWith("http") && it.body.contains("atlassian") ? null : it }.findAll { it != null }

