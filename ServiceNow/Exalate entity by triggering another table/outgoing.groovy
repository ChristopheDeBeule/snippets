if(entity.tableName == "sc_request") {
    def RITM = httpClient.get("/api/now/table/sc_req_item?sysparm_query=request=${entity.id}&sysparm_fields=number,sys_id")?.result
    if(RITM){
        String key = RITM instanceof ArrayList ? RITM[0]?.number : RITM?.number
        String id = RITM instanceof ArrayList ? RITM[0]?.sys_id : RITM?.sys_id

        def sc_req_item = new com.exalate.basic.domain.BasicIssueKey(id, key, "sc_req_item")
        syncHelper.exalate(sc_req_item)
    }
}