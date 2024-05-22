// Ado Client Class Start
 /* AdoClient Class here */
// Ado Client Class End

// Create iteration depending on sprint
// We make sure the project name is found
def encode(String projectKey) {
    if (!projectKey) {
        return projectKey
    } else {
        return java.net.URLEncoder.encode(projectKey, java.nio.charset.StandardCharsets.UTF_8.toString())
    }
}
def projectName = encode(workItem.project?.key) ?: encode(workItem.projectKey)
projectName = projectName.replace("+", "%20") // Name with spaces contains '+' char

// This function gets every iteration path in your project and returns them
def getClassificationnodesADO(def projectName, int classificationnodes = 0){ 
    // get the area and iteration values
    def existingArea = httpClient.get("/${projectName}/_apis/wit/classificationnodes?\$depth=2&api-version=5.0-preview.2",true)
    // this return only the iteration path values
    if (classificationnodes > 1) debug.error("The 2nd parameter for getClassificationnodesADO('${projectName}', \"\"${classificationnodes}\"\") it to high, please enter 0 (Area Path) or 1 (Iteration Path).")
    return existingArea.value[classificationnodes]; // 1 is for the iteration path 0 is for the area path
}

// we search in the iteration children (the values under the project name), we add those names in the tmp list
def tmp = []

// The function getClassificationnodesADO returns the area or iteration path in ADO 
// It expects 2 parameters if you give one it will by default return the area path 
// The second parameter is an int that will return the area or iteration path
getClassificationnodesADO(projectName, 1)?.children.each{ i -> tmp += i?.name}


def shortPost(def sprint, def projectName){
    def sprintStartDate = sprint?.startDate.toString().replace(" ","T")
    def sprintendDate = sprint?.endDate.toString().replace(" ","T") 
    def sprintName = sprint?.name?.toString()

    def url = "/${projectName}/_apis/wit/classificationnodes/Iterations".toString()
    def body =  "{\"name\": \"${sprintName}\",\"attributes\": {\"startDate\": \"${sprintStartDate}\",\"finishDate\": \"${sprintendDate}\"}}"
    httpClient.post(url, body)
}

def replaceAndRemoveSpecialChars(String input) {
    // Replace & with "and"
    String replacedString = input.replaceAll("&", "and")
    
    // Define a string containing all special characters to be replaced, excluding &
    // since it's already been replaced with "and"
    String specialChars = '#\\$%\\*\\+\\|:"\\?/\\\\><'
    
    // Use replaceAll to remove the special characters. Since some characters have special meaning in a regex pattern,
    // they need to be escaped with double backslashes. In Groovy strings, a single backslash is used to escape characters,
    // so you see quadruple backslashes for the regex pattern.
    return replacedString.replaceAll("[$specialChars]", "")
}

def addSprintToIteration(def sprint, def projectName){
    def adoClient = new AdoClient(httpClient, nodeHelper, debug)
    def sprintStartDate = sprint?.startDate.toString().replace(" ","T")
    def sprintendDate = sprint?.endDate.toString().replace(" ","T") 
    def sprintName = replaceAndRemoveSpecialChars(sprint?.name?.toString())

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
}

def sprint = replica.customFields."Sprint".value[0]
// we remove any special characters that ADO does not allow in the iteration path
def checkedSprintName = replaceAndRemoveSpecialChars("${sprint?.name?.toString()}")
def projectNameWithApace = projectName
// we will remove the %20 to a space cause the project name does not need it, you only need this for the rest API.
projectNameWithApace = projectNameWithApace.replace("%20"," ")

// here we check if the sprint is not null and that tmp (all iteration paths) does not exsists already.
// we create a new iteration path if the sprint does not exsists already
if(sprint?.name != null && !tmp.contains(checkedSprintName)){
    addSprintToIteration(sprint, projectName)
    workItem.iterationPath = "${projectNameWithApace}\\${checkedSprintName}"
}else if (sprint?.name != null && tmp.contains(checkedSprintName)){
    workItem.iterationPath = "${projectNameWithApace}\\${checkedSprintName}"
}