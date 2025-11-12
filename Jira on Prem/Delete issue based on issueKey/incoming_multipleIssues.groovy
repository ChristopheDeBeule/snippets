import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.event.type.EventDispatchOption

void deleteIssuesByKey(List<String> issueKeys) {
    def issueManager = ComponentAccessor.getIssueManager()
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    //throw new Exception("${user}")

    issueKeys.each{ key ->
        def issue = issueManager.getIssueByCurrentKey(key)
        if (issue) {
            issueManager.deleteIssue(user, issue as MutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
        } else {
            // 🧨 Throwing exception if not found, you can remove this if it throws an error and ignore it.
            throw new Exception("Issue not found: ${key}")
        }
    }
    
}

if(!firstSync){
    // Provide a String list of issue Keys
    deleteIssuesByKey(["ABC-1", "ABC-2"])
}