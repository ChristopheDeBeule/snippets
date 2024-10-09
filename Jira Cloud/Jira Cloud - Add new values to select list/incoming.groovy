import groovy.json.JsonOutput

// Get all fields
def customFieldsFound = httpClient.get("/rest/api/3/issue/${issue.key}/editmeta")?.fields

// Find the field by its name
def colorFieldEntry = customFieldsFound.find { entry ->
  entry.value.name == "Color" // select list name (Case sensitive)
}
// get all values in from the select list
List selectListValues = null
def contextId = null
def colorField = null

if (colorFieldEntry) {
  colorField = colorFieldEntry.value as Map
  selectListValues = colorField?.allowedValues?.value ?: []
  contextId = httpClient.get("/rest/api/3/field/${colorField?.key}/context")?.values[0]?.id
}
String newValue = "Pink" // get this from the remote site. (replica.CF_Name)
def body = [
  "options": [
    [
      "value": "${newValue}"
    ]
  ]
]

if(!selectListValues.contains(newValue) && contextId){
  httpClient.post("/rest/api/3/field/${colorField?.key}/context/${contextId}/option", JsonOutput.toJson(body))
}
