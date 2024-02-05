Map<String, String> buildVarsMap(requestItemNumber, limitResult) {
    
    def Map<String, Object> result = [:]

    // lookup all options associated to this number
    //debug.error(requestItemNumber.toString())
    def optionList = httpClient.get("/api/now/table/sc_item_option_mtom?sysparm_query=request_item.number=${requestItemNumber}&sysparm_limit=${limitResult}")    
    //debug.error(optionList.toString())
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
        
        result.put ( itemOptionNewDetails.result.question_text, optionDetails.result.value)        
    }
    return result
}

if(entity.tableName == "sc_task") {

    replica.summary = entity.short_description
    replica.key = entity.key
    replica.component = "Other/Comp"
    replica.description = entity.description
    replica.priority = entity.priority
    replica.attachments = entity.attachments
    replica.customField = "Other"
    
    def ritmKey = entity.request_item?.display_value.toString()
    replica.variables = buildVarsMap("${ritmKey}", 20)
}