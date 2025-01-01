import groovy.json.JsonOutput

// Define constants for the project and issue type
def PROJECT_KEY = "DEMO"
def ISSUE_TYPE = "Task"

// User mapping for GitHub usernames to Jira emails
def userMap = [
    "ChristopheDeBeule": "christophe.debeule@exalate.com",
    "RazeSevi": "christophe.s.debeule@gmail.com"
]

// Function to create and link a Jira issue
def createAndLinkIssue(def assigneeKey, int count) {
    try {
        def assignee = nodeHelper.getUserByEmail(assigneeKey)
        if (!assignee) {
            assignee = null
        }else{
            assignee = assignee.key
        }

        def bodyCreateIssue = [
            "fields": [
                "project"  : ["key": PROJECT_KEY],
                "summary"  : "${issue.summary} - ${count}",
                "issuetype": ["name": ISSUE_TYPE],
                "assignee" : ["accountId": "${assignee}"]
            ]
        ]
        def newIssue = httpClient.post("/rest/api/3/issue/", JsonOutput.toJson(bodyCreateIssue))
        
        if (newIssue) {
            def bodyLinkIssues = [
                "outwardIssue": ["key": "${newIssue.key}"],
                "inwardIssue" : ["key": "${issue.key}"],
                "type"        : ["name": "Relates"]
            ]
            httpClient.post("/rest/api/3/issueLink", JsonOutput.toJson(bodyLinkIssues))
        } else {
            throw new Exception("Failed to create new Jira issue")
        }
        
        return newIssue
    } catch (Exception e) {
        throw new Exception("Failed to create or link Jira issue: ${e.message}")
    }
}

// Function to handle creating issues for multiple assignees
def newIssuePerAssignee(def assigneeList) {
    def issueList = []
    def count = issue.customFields."Total Salesforce Cases"?.value ?: 0
    
    if (assigneeList.size() == 1) {
        // Set assignee if there's only one
        def assignee = nodeHelper.getUserByEmail(userMap[assigneeList[0]?.displayName])
        if (assignee) {
            issue.assignee = assignee
        } 
    } else {
        def linkedIssues = getLinkedIssues()
        def linkedIssueKeys = linkedIssues.collect { it.outwardIssue?.key }
        
        if (linkedIssueKeys.isEmpty()) {
            // No linked issues found, create new issues for all additional assignees
            (1..<assigneeList.size()).each { i ->
                def assigneeEmail = userMap[assigneeList[i]?.displayName]
                if (assigneeEmail) {
                    count++
                    issueList.add(createAndLinkIssue(assigneeEmail, count))
                }
            }
        } else {
            handleExistingLinkedIssues(linkedIssueKeys, assigneeList, count, issueList)
        }
        
        // Set the original Jira issue assignee to the first GitHub assignee
        def firstAssigneeEmail = userMap[assigneeList[0]?.displayName]
        if (firstAssigneeEmail) {
            issue.assignee = nodeHelper.getUserByEmail(firstAssigneeEmail)
        } 
    }
    
    issue.customFields."Total Salesforce Cases"?.value = count
    return issueList
}

// Helper function to get linked issues
def getLinkedIssues() {
    try {
        def linkedIssues = httpClient.get("/rest/api/3/issue/${issue.key}")?.fields?.issuelinks
        return linkedIssues ?: []
    } catch (Exception e) {
        throw new Exception("Failed to retrieve linked issues: ${e.message}")
    }
}

// Helper function to handle existing linked issues
def handleExistingLinkedIssues(def linkedIssueKeys, def assigneeList, int count, def issueList) {
    linkedIssueKeys.each { linkedIssueKey ->
        try {
            def issueAssignee = httpClient.get("/rest/api/3/issue/${linkedIssueKey}")?.fields?.assignee?.accountId
            def userEmail = nodeHelper.getUser(issueAssignee)?.email
            def displayName = userMap.find { it.value == userEmail }?.key
            
            if (!assigneeList*.displayName.contains(displayName)) {
                def assigneeEmail = userMap[displayName]
                if (assigneeEmail) {
                    count++
                    issueList.add(createAndLinkIssue(assigneeEmail, count))
                } else {
                    throw new Exception("Assignee not found during linked issue handling: ${displayName}")
                }
            }
        } catch (Exception e) {
            throw new Exception("Failed to handle existing linked issue: ${e.message}")
        }
    }
}

// Initial setup
if (firstSync) {
    issue.projectKey = PROJECT_KEY
    issue.typeName = ISSUE_TYPE
    issue.summary = replica.summary
    store(issue)
}

// Syncing and issue creation logic
def newIssues = newIssuePerAssignee(replica.assignees)

if (!newIssues.isEmpty()) {
    issue.customFields."Salesforce Case Reference".value += newIssues.toString()
    
    try {
        syncHelper.syncBackAfterProcessing()
    } catch (Exception e) {
        log.error("Failed to sync back after processing: ${e.message}")
    }
}