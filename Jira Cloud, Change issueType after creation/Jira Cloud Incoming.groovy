// Gets issueTypes in project
def list = httpClient.get("/rest/api/3/project/${issue.projectKey}")?.issueTypes
def issueTypeIdMap = [:]

for(int i = 0; i < list.size(); i++){
    // Create a Type map with right Id's dynamically from rest API, gets every issueType with id from the projectKey
   issueTypeIdMap[list?.name[i]] = list?.id[i]
}
// the type variable is how the issueTypes reflect in that specific project
def type = replica.customFields."Demo"?.value
String domain = "/rest/api/3/issue/${issue.key}/"
String data = "{\"fields\":{\"issuetype\":{\"id\":${issueTypeIdMap[type]}}}}"
//debug.error(issueTypeIdMap.toString())
httpClient.put(domain, data)

/*
// After the map creation it will look something like this
[
Task:10005, 
Sub-task:10006, 
Story:10004, 
Bug:10007, 
Pull Request:10014,
Epic:10000, 
Feature:10016, 
Request:10017
]
*/