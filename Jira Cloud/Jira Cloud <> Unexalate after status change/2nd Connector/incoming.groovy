if(replica.status.name == "your status"){
    // set a custom field with a value that can be synced to Jira
    issue.customField."CF Name".value = "true"
    syncHelper.syncBackAfterProcessing()
}