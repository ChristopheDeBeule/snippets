// Old Way
// def parentLink = httpClient.get("/rest/api/3/issue/${issue.key}").fields?.parent?.key
// replica."Epic Link" = parentLink



// New Way

replica.parentId = issue.parentId
