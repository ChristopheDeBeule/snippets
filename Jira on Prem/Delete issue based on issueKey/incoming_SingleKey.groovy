import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.event.type.EventDispatchOption

void deleteIssuesByKey(String key) {
    def issueManager = ComponentAccessor.getIssueManager()
    def user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
    //throw new Exception("${user}")
    def issue = issueManager.getIssueByCurrentKey(key)
    if (issue) {
        issueManager.deleteIssue(user, issue as MutableIssue, EventDispatchOption.DO_NOT_DISPATCH, false)
    } else {
        // 🧨 Throwing exception if not found
        throw new Exception("Issue not found: ${key}")
    }
}

if(!firstSync){
    deleteIssuesByKey(issue.key)
}