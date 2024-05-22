def parentLink = httpClient.get("/rest/api/3/issue/${issue.key}").fields?.parent?.key
replica."Epic Link" = parentLink