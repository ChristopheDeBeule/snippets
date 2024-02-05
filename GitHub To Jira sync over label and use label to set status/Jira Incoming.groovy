// Filters out "In Progress from the labels and will change the status to in progress"
def state = ""
// iterates over every label 
for (int i = 0; i < replica.labels.size(); i++){
   // checks if the label is not "In Progress" and add it to issue.labels it will also remve white spaces because Jira labels cannot contain whitespaces
   if (replica.labels.label[i] != "In Progress"){
      issue.labels += nodeHelper.getLabel(replica.labels.label[i].replaceAll(" ","-").toString())
   }
   // if the label is "In Progress" it will be set to the state variable that will be used to check if the status needs to be set to "In progress"
   else{
      state = replica.labels.label[i]
   }
}

def statusMapping = [
   "Open":"Open", 
   "Closed":"Done"
]
def remoteStatusName = replica.status.name

if (state){
   issue.status = state
}else{
   issue.setStatus(statusMapping[remoteStatusName] ?: "Open")
}