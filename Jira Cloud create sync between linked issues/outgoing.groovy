def linkedIssue = []

def inwardIssue = httpClient.get("/rest/api/3/issue/${replica?.key}/")?.fields?.issuelinks?.inwardIssue[0]
def outwardIssue = httpClient.get("/rest/api/3/issue/${replica?.key}/")?.fields?.issuelinks?.outwardIssue[0]

//debug.error("Inward = ${inwardIssue.toString()}, Outward = ${outwardIssue.toString()}")

if (inwardIssue){
    replica.linkedIssue = inwardIssue
}else{
     replica.linkedIssue = outwardIssue
}
