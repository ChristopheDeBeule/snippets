def sysId = httpClient.get("/api/now/table/incident?sysparm_query=number=${entity.key}")?.result?.sys_id[0]
if(sysId){
    def parentID = httpClient.get("/api/now/table/incident/${sysId}?sysparm_display_value=entity.parent_incident")?.result?.parent_incident
    if(parentID){
        replica.parentId = parentID?.value
    }
}