// This will show you how to use the httpClient in Salesforce

// GET
// entity.Id = your case or entity ID looks something like: 500O5000007focBIAA
def getVariable = httpClient.get("/services/data/v52.0/chatter/feeds/record/${entity.Id}/feed-elements")

// You can also use the SOQL 
def getVariable = httpClient.get("/services/data/v60.0/query/?q=SELECT+name+from+Account") // something like this.


// TODO: Add put & post 