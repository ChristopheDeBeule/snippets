// Ado Client Class Start
 /* AdoClient Class here */
// Ado Client Class End

// Create iteration depending on sprint
// This functions gets every iteration path in your project and returns them
def encode(String projectKey) {
    if (!projectKey) {
        return projectKey
    } else {
        return java.net.URLEncoder.encode(projectKey, java.nio.charset.StandardCharsets.UTF_8.toString())
    }
}
def projectName = encode(workItem.project?.key) ?: encode(workItem.projectKey)
projectName = projectName.replace("+", "%20") // Name with spaces contains '+' char

def getAreaPathValuesADO(def projectName){
    // get the area and iteration values
    def existingArea = httpClient.get("/${projectName}/_apis/wit/classificationnodes?\$depth=2&api-version=5.0-preview.2",true)
    // this return only the iteratio values
    return existingArea.value[1]; // 1 is for the iteration path 0 is for the area path
}

// we search in the iteration children (the values under the project name), we add those names in the tmp list
def tmp = []
getAreaPathValuesADO(projectName)?.children.each{ i -> tmp += i?.name}

def addSprintToIteration(def sprint, def projectName){
    // Get Area Block Start
    def adoClient = new AdoClient(httpClient, nodeHelper, debug)
    def sprintStartDate = sprint?.startDate.toString().replace(" ","T")
    def sprintendDate = sprint?.endDate.toString().replace(" ","T") 
    def sprintName = sprint?.name?.toString()

    def time = "2017-04-24T00:00:00Z"
    adoClient
        .http (
            "POST",
            "/${projectName}/_apis/wit/classificationnodes/Iterations".toString(),
            ["api-version":["7.1"]],
            "{\"name\": \"${sprintName}\",\"attributes\": {\"startDate\": \"${sprintStartDate}\",\"finishDate\": \"${sprintendDate}\"}}",            
            ["Content-Type":["application/json"]]
            ) { res ->
                if(res.code >= 400) {
                    debug.error("Failed to POST /${projectName}/_apis/wit/classificationnodes/Iterations RESPONSE: ${res.code} ${res.body}")
                }
                else {
                    (new groovy.json.JsonSlurper()).parseText(res.body)
                }
            }

    // Get Area Block End
}
// here we check if the sprint is not null and that tmp (all iteration paths) does not exsists already.
// we create a new iteration path if the sprint does not exsists already
def sprint = replica.customFields."Sprint".value[0]

if(sprint?.name != null && !tmp.contains(sprint?.name?.toString())){
    addSprintToIteration(sprint, projectName)
    workItem.iterationPath = "${projectName}\\${sprint?.name?.toString()}"
}else if (sprint?.name != null && tmp.contains(sprint?.name?.toString())){
    workItem.iterationPath = "${projectName}\\${sprint?.name?.toString()}"
}