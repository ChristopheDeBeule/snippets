import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.customfields.option.Option
import com.atlassian.jira.issue.customfields.manager.OptionsManager


// Method to get add new field value to your select list
def addNewOptionToSelectList(String issueKey, String newOptionValue, String customFieldID) {

    def issueManager = ComponentAccessor.issueManager
    def mainIssue = issueManager.getIssueByCurrentKey(issueKey)
    def customFieldManager = ComponentAccessor.getCustomFieldManager()
    
    String cutomFieldID = customFieldID.contains("customfield_") ? customFieldID : "customfield_"+customFieldID
    CustomField customField = customFieldManager.getCustomFieldObject(cutomFieldID)
    def optionsManager = ComponentAccessor.getComponent(OptionsManager)

    // Get the value of the custom field from the issue
    def customFieldValue = mainIssue.getCustomFieldValue(customField)
    def customFieldName = customField.getName()

    // Get the options for the custom field (only if the custom field is a select list)
    def fieldConfig = customField.getRelevantConfig(mainIssue)  // Get the configuration of the custom field
    def options = optionsManager.getOptions(fieldConfig)
    List optionsValues = options ? options.value : []
    if(!optionsValues.isEmpty()){
        if(!optionsValues.contains(newOptionValue)) optionsManager.createOption(fieldConfig, null, null, newOptionValue)
        return "Option '${newOptionValue}' Already exists in Field '${customFieldName}' with ID '${customFieldID}', with values<br>${optionsValues}"
    }
}

String testOptionAdded = "TestValueColor" // get this value from the remote side ex: replica.customFields."custom field name".value
// String issueKey, String newOption, String custom field ID customfield_xxxxx or xxxxx
addNewOptionToSelectList(issue.key, testOptionAdded, "customfield_10116")
issue.customFields."Color".value = testOptionAdded