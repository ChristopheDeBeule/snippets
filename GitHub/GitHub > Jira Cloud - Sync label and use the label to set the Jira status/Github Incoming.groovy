//Set label in progress in github
if (replica.status.name == "In Progress"){
    issue.labels += nodeHelper.getLabel("In Progress") // Make sure the Label "In Progress" exists on your GH
}
issue.labels       = replica.labels
