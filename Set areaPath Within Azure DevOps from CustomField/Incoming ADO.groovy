// import the AdoClient Class here
// AdoClient Start

// AdoClient End
if(firstSync){
   // Set type name from source entity, if not found set a default
   workItem.projectKey  =  "Christophe"
   workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
}

workItem.summary      = replica.summary
workItem.description  = replica.description
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority

// This function will get all the values from the rest api in ADO
// The hierarchy has a children array within a children array look in the "Example of areaPath hierarchy" script.

def getAreaPathValuesADO(){
    // Get Area Block Start
    def adoClient = new AdoClient(httpClient, nodeHelper, debug)
    def encode = {
        str ->
        if (!str) { str }
        else{
            java.net.URLEncoder.encode(str, java.nio.charset.StandardCharsets.UTF_8.toString())
        }
    }
    def projectName = encode(workItem.project?.key) ?: encode(workItem.projectKey)
    // "${projectName}/_apis/wit/classificationnodes?ids=4&$depth=10"
    projectName = projectName.replace("+", "%20") // Name with spaces contains '+' char
    def existingArea = adoClient
        .http (
            "GET",
            "/${projectName}/_apis/wit/classificationnodes".toString(),
            ["api-version":["5.0"], "\$depth":["100"]],
            null,
            ["Accept":["application/json"]]
            ) { res ->
                if(res.code >= 400) {
                    debug.error("Failed to GET /${projectName}/_apis/wit/classificationnodes?ids=4&\$depth=100 RESPONSE: ${res.code} ${res.body}")
                }
                else {
                    (new groovy.json.JsonSlurper()).parseText(res.body)
                }
            }
    return existingArea.value[0];
    // Get Area Block End
}

// This Function will check if the parent (the custom field value) exsists in the found parents of the area path
def setApplicationFeatureCategoryField(String prefix){ 
    def parent = replica.customFields."Application/Feature Category"?.value?.parent?.value  
    def child = replica.customFields."Application/Feature Category"?.value?.child?.value  

    if (parent == null) { return prefix }
    def areaPathValue = getAreaPathValuesADO()
    def parents = []
    def childs = []

    for (int i = 0; i < areaPathValue.children.size(); i++){
        parents += areaPathValue.children[i].name
        if (areaPathValue.children[i].children != null){
            for (int j = 0; j < areaPathValue.children[i].children.size(); j++){
                childs += areaPathValue.children[i].children[j].name
            }
        }
    }
    // Will return the prefix\parent value if parent is not empty and child is empty.
    // checks if the parent value exists within parents if not return prefix
    // Now no errors will be thrown if the value was not found
    if (!parents.contains(parent) || (!parents.contains(parent) && !childs.contains(child))){ 
        return prefix
    }
    if ((parents.contains(parent) && !childs.contains(child))){ 
        return "${prefix}\\${parent}"  
    }
    // Will return prefix\parent\child value if the parent value is not empty.
    if (parents.contains(parent) && childs.contains(child)){
        return "${prefix}\\${parent}\\${child}"
    }
    // If the parent value is empty it will return the prefix only.
    return prefix
}

// Set your workItem.areaPath to the setApplicationFeatureCategoryField(String prefix) value. 
// Prefix is you project name in ADO
def prefix = "Prefix Name (Project name)" 
workItem.areaPath = setApplicationFeatureCategoryField(prefix)



