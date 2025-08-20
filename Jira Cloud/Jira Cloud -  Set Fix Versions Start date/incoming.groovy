import java.text.SimpleDateFormat
import groovy.json.JsonOutput 


// Add this in your config
def project = nodeHelper.getProject(issue.projectKey)
// Set the fixed versions
issue.fixVersions = replica.fixVersions.collect { 
  v -> 
  // If the verison already exists set it to the one that is found
  nodeHelper.createVersion(issue, v.name, v.description) ?: nodeHelper.getVersion(v.name, project)
}.findAll{it != null}


List setStartDate = []
// get all the versions to see if they have a start date.
// If not add these Id's to the list
issue.fixVersions.each{
  def tmp = httpClient.get("/rest/api/3/version/${it?.id}")?.startDate
  if(!tmp){
    setStartDate.add(it?.id)
  }
}

// If the list has ID's set the start date for all of them.
if(!setStartDate.isEmpty()){
  def sdf = new SimpleDateFormat("yyyy-MM-dd")
  String currentDate = sdf.format(new Date())

  Map body = [
    "startDate":currentDate
  ]
  setStartDate.each{
    httpClient.put("/rest/api/3/version/${it}", JsonOutput.toJson(body))
  }
}