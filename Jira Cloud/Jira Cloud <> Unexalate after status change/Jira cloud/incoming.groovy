// Now with the value we get from the other side (this can be any field)
// We set a custom field in Jira and have an automation in place to check if the field value is changed

// Set the Jira custom field if the remote field has the right value
if(replica.customField."CF Name"?.value == "true"){
    issue.customField."CF Name".value = "true"
}
// This will trigger your automation to transition the issue and this will unexalate the issue.