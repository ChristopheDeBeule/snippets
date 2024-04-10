// This function will replace the User ID to the User name in the BuildVar Function.
def getUserNames(def input) {
    def pattern = ~/^[\dA-Z]+[a-z\d]+(,[\dA-Z]+[a-z\d]+)*$/
    def names = []
    // If you have multiple values (picklist) these are comma seperated we check if the string has a comma and then add them into a list
    // We loop over this new list and changes each value separate.
    if(input.contains(",")){
        input.split(',').each { sysId ->
            def getUserResponse = httpClient.get("/api/now/table/sys_user?sysparm_query=sys_id%3D${sysId}&sysparm_display_value=true&sysparm_fields=name&sysparm_limit=1")
            //debug.error(getUserResponse.result[0]?.name.toString().toString())
            if (getUserResponse?.result) {
                names << getUserResponse.result[0]?.name.toString()
            } else {
                names << null 
            }
        }
        // After we added all new values (User names) into a new list we join them and return a string.
        return names.join(',')
    }
    if (input ==~ pattern) {
        def getUserName = httpClient.get("/api/now/table/sys_user?sysparm_query=sys_id%3D${input}&sysparm_display_value=true&sysparm_fields=name&sysparm_limit=1")
        return  getUserName?.result[0]?.name.toString()
    } else {
        return null
    }
}


// This function will fetch all the variables in the given RITM
Map<String, String> buildVarsMap(requestItemNumber, limitResult) {
    def Map<String, Object> result = [:]
    // lookup all options associated to this number
    //debug.error(requestItemNumber.toString())
    def optionList = httpClient.get("/api/now/table/sc_item_option_mtom?sysparm_query=request_item.number=${requestItemNumber}&sysparm_limit=${limitResult}")   
   // debug.error(optionList.toString())
    if (!optionList || !optionList.result) return null  // ignore if there are no results
    // For each of the options, lookup corresponding question and add to the result map
    optionList.result.each {
        def optionSysId = it.sc_item_option?.value
        def optionDetails = httpClient.get("/api/now/table/sc_item_option/${optionSysId}")
        if (!optionDetails || !optionDetails.result) return // ignore - the option itself is not found
        def itemOptionNew = optionDetails?.result?.item_option_new?.value
        if (!itemOptionNew) return  // ignore - link to question not found
        def itemOptionNewDetails = httpClient.get("/api/now/table/item_option_new/${itemOptionNew}")
        if (!itemOptionNewDetails || !itemOptionNewDetails.result) return // ignore - the question is not found
        def optionsDetails = getUserNames(optionDetails.result.value) ?: optionDetails.result.value
        result.put ( itemOptionNewDetails.result.question_text, optionsDetails)       
    }
    return result
}
// Note this is for a catalog Task you can also just sync over an request item.
if(entity.tableName == "sc_task") {

    replica.summary = entity.short_description
    replica.key = entity.key
    replica.component = "Other/Comp"
    replica.description = entity.description
    replica.priority = entity.priority
    replica.attachments = entity.attachments
    replica.customField = "Other"
    //def ritmKey = requestItem.number // If the entity is a request Item
    def ritmKey = entity.request_item?.display_value.toString() // If the entity is a Catalog Task
    replica.variables = buildVarsMap("${ritmKey}", 20)
}