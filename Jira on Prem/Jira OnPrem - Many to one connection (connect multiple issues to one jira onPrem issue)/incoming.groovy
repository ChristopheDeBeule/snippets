import com.atlassian.jira.component.ComponentAccessor
 
def remoteIssueUrn = replica.customFields."Demo"?.value

// This will check if there is a remote issue URN so we can do a replace otherwise you do a replace on a null object which can cause errors.
if(remoteIssueUrn){
    // Removes all whitespace from the string.
    remoteIssueUrn = remoteIssueUrn.replaceAll("s","")
}
 
if(firstSync && remoteIssueUrn){
   def issueManager = ComponentAccessor.issueManager
   def mainIssue = issueManager.getIssueByCurrentKey(remoteIssueUrn)
  
    if (!mainIssue){    
       return 
    }else{
        issue.projectKey   = "DEMO" // Your project key.
        issue.typeName     = "Task" // Your type name.
         // Connect through Exalate
        def issueKey = new com.exalate.basic.domain.BasicIssueKey(mainIssue.id, mainIssue.key)
        def hubObjVersion = new com.exalate.basic.domain.BasicSemanticVersion(1, 0, 0);
        def hubObjectHelperFactory = com.atlassian.jira.component.ComponentAccessor
          .getOSGiComponentInstanceOfType(com.exalate.api.hubobject.IHubObjectHelperFactory.class)
        def hubObjectHelper = hubObjectHelperFactory.get(hubObjVersion)
        def hubIssue = hubObjectHelper.getHubIssueOnOutgoingProcessor(issueKey)
        hubIssue.getProperties().each { k, v ->
            issueBeforeScript[k] = v
            if (v instanceof Map) {
                def newV = [:]
                newV.putAll(v)
                issue[k] = newV
            } else {
                issue[k] = v
            }
        }
        def iterator = hubIssue.entrySet().iterator()
        while (iterator.hasNext()) {
            def entry = iterator.next()
            def k = entry.key
            def v = entry.value
            // Update issueBeforeScript and issue
            issueBeforeScript.put(k, v)
            issue.put(k, v)
        }
        issueBeforeScript.id = null
        issue.id = mainIssue.id
        issue.key = mainIssue.key
        issue.customKeys.somethingIsChanged = true
    }
}
// This will create a new issuen if there is no remote issueUrn.
if(!remoteIssueUrn && firstSync){ 
    issue.projectKey = "DEMO" // Your project key
    issue.typeName = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
    issue.summary = replica.summary
    issue.description = replica.description
    // Enter below the fields you need
}
// This will keep updating the issue if it's not connected
if (!remoteIssueUrn && !firstSync){
    // Enter the fields you need below.
}