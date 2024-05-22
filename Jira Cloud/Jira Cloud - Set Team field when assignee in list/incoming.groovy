
String teamMembers = [
    "christophe.debeule@exalate.com"
]
def teamId = "ee220d3c-5f37-47ba-a1e3-d8fae4097cc3" // This is the team ID you can find this in your Jira teams.
// Custom Field customfield_10001 is the team ID you can find this in your custom fields.
def teamData = "{\"fields\": {\"customfield_10001\" : \"${teamId}\"}}"
// We check if we have an assignee, you cannot to a contains with a null value.
if (replica.assignee?.email){
    if (teamMembers.contains(replica.assignee?.email)){
        httpClient.put("/rest/api/3/issue/${issue.key}/", teamData)
    }
}

// Jira API DOCS
// https://developer.atlassian.com/platform/teams/components/team-field-in-jira-rest-api/#creating-or-updating-an-issue-with-team
