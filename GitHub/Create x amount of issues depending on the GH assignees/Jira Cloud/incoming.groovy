import groovy.json.JsonOutput 

if (firstSync) {
    issue.projectKey  = "DEMO"
    // Set the same issue type as the source issue. If not found, set a default.
    issue.typeName    = "Task"
    issue.summary      = replica.summary
    store(issue)
}
def userMap = [
  "ChristopheDeBeule":"christophe.debeule@exalate.com",
  "RazeSevi":"christophe.s.debeule@gmail.com"
]

issue.summary      = replica.summary
issue.description  = replica.description
issue.comments     = commentHelper.mergeComments(issue, replica)
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.labels       = replica.labels

// Create a new issue
def createAndLinkIssue(def assigneeKey, int count){

  def bodyCreateIssue = [
    "fields": [
      "project": [
        "key": "${issue.projectKey}"
      ],
      "summary": "${issue.summary} - ${count}",
      "issuetype": [
        "name": "${issue.typeName}"
      ],
      "assignee": [
        "accountId": "${nodeHelper.getUserByEmail(assigneeKey)?.key}"
      ]
    ]
  ]
  def newIssue =  httpClient.post("/rest/api/3/issue/", JsonOutput.toJson(bodyCreateIssue))
   
  if(newIssue){
    def bodyLinkIssues = [
      "outwardIssue": [
        "key": "${newIssue.key}"
      ],
      "inwardIssue": [
        "key": "${issue.key}"
      ],
      "type": [
        "name": "Relates"
      ]
    ]
    
    // Link OG gh ticket to New created issue
    httpClient.post("/rest/api/3/issueLink", JsonOutput.toJson(bodyLinkIssues))
  }
  return newIssue
}

def newIssuePerAssignee(def assigneeList){
  def issueList = []
  def userMap = [
    "ChristopheDeBeule":"christophe.debeule@exalate.com",
    "RazeSevi":"christophe.s.debeule@gmail.com"
  ]

  def count = issue.customFields."Total Salesforce Cases"?.value
  if(!count){
    count = 0
  }else{
    // cast to an int
    count = (int)count
  }
  if (assigneeList.size() <= 1){
    // Create the issue and set assignee if there is only one assignee
    issue.assignee = nodeHelper.getUserByEmail(userMap[assigneeList[0]?.displayName])
  }else{
    // add to count cause the og ticket is not in the count
    if(count != assigneeList.size() -1){

      def linkedIssues = httpClient.get("/rest/api/3/issue/${issue.key}")?.fields?.issuelinks
      def linkedIssueKeys = []
      if(linkedIssues){
          // Get a list of all teh issueKeys
        linkedIssues.each{
          linkedIssueKeys += it.outwardIssue?.key
        }
      }
      if(linkedIssueKeys.isEmpty()){
        // only create new issue so start with 1
        for (int i = 1; i < assigneeList.size(); i++){
          count++
          issueList.add(createAndLinkIssue(userMap[assigneeList[i]?.displayName], count))
        } 
      }else{
        for(int i=0; i < linkedIssueKeys.size(); i++){
          def issueAssignee = httpClient.get("/rest/api/3/issue/${linkedIssueKeys[i]}")?.fields?.assignee?.accountId
          def user = nodeHelper.getUser(issueAssignee)?.email
          // Revert the mapping to find the assignee displayname with the Email we got from the linked issue assignee
          def displayName = userMap.find { it.value == user }?.key
          def assigneeDisplayName = assigneeList.collect{it.displayName}
          if(!assigneeDisplayName.contains(displayName)){
            count++
            issueList.add(createAndLinkIssue(userMap[user], count))
          }
        }
      }
      // set the OG ticket assignee to the 1st assignee in GH
      issue.assignee = nodeHelper.getUserByEmail(userMap[assigneeList[0]?.displayName])
    }
  }
  issue.customFields."Total Salesforce Cases"?.value = count
  return issueList
}


def newIssues = newIssuePerAssignee(replica.assignees)

if(!newIssues.isEmpty()){
  issue.customFields."Salesforce Case Reference".value += newIssues.toString()

  // TODO: Find a way to connect all new created issues to GH with Exalate 
  syncHelper.syncBackAfterProcessing()
}



//////
// if (replica.assignees){
//   if(!replica.description.contains())
//   issue.assignee = nodeHelper.getUserByEmail(userMap[replica.assignees[0].displayName])
// }

// if (issue.assignee){
//   issue.customFields."Demo".value = replica.assignees[0].displayName
// }



// if(!firstSync && syncRequest.remoteSyncEventNumber==1){
//   if (replica.assignees){
//     if(!replica.description.contains())

//     def issueList = []
//     for(int i = 0; i < replica.assignees; i++){
//       if (!replica.description.contains(replica.assignees[i].displayName)){
//         issueList.add(createAndLinkIssue(userMap[replica.assignees[i].displayName]))
//         break;
//       }
//     }
//     throw new Exception("${issueList}")
//     //issue.assignee = nodeHelper.getUserByEmail(userMap[replica.assignees[0].displayName])
//   }
  
// }


// user needs to be mapped