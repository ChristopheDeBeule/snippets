def setEpicLink(){
   def remoteIssue = nodeHelper.getLocalIssueFromRemoteUrn(replica?."Epic Link")
   issue.customFields."Epic Link".value = remoteIssue
}

if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   issue.typeName     = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
   issue.summary      = replica.summary
   store(issue)
   setEpicLink()
}

if(!firstSync){
   setEpicLink()
}