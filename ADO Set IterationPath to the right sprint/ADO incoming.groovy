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

// here we check if the sprint is not null and that tmp (all iteration paths) does not exsists already.
// we create a new iteration path if the sprint does not exsists already
def sprint = replica.customFields."Sprint".value[0]


def checkedSprintName = replaceAndRemoveSpecialChars("${sprint?.name?.toString()}")
//debug.error()
if(sprint?.name != null && !tmp.contains(checkedSprintName)){
    addSprintToIteration(sprint, projectName)
    workItem.iterationPath = "${projectName}\\${checkedSprintName}"
}else if (sprint?.name != null && tmp.contains(checkedSprintName)){
    workItem.iterationPath = "${projectName}\\${checkedSprintName}"
}