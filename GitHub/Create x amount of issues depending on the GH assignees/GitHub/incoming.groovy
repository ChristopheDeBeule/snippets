import groovy.json.JsonSlurper

if (firstSync) {
    issue.repository   = "ChristopheDeBeule/Exalate"
}
issue.summary         = replica.summary

// When we have a value from the other side we will create a json object so we can easaly access the values of all the created jira issues.
def stringToJson = replica.createdIssuesValues
if (stringToJson){
 
  def jsonSlurper = new JsonSlurper()
  def regex = /(\w+):([^\[\],]+)/
  // Add "" around the key and the valule
  def output = stringToJson.replaceAll(regex) { match, key, value ->
    "\"${key}\":\"${value}\""
  }
  // replace the inner [] with {}
  output = output.replaceAll(/\[(\s*["'\w]+:.*?)]/, '{$1}')
  // "[["id":"12352", "key":"DEMO-1563"],["id":"12353", "key":"DEMO-1564"]]"
  def jsonObjectList = jsonSlurper.parseText(output)
  //debug.error("${jsonObjectList}")
}

