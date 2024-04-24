import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.bc.issue.search.SearchService 
import com.atlassian.jira.web.bean.PagerFilter
// Search services 
def searchService = ComponentAccessor.getComponent(SearchService.class)
def jcl = ComponentAccessor.classLoaderdefjqlQueryParserClass = jcl.loadClass("com.atlassian.jira.jql.parser.JqlQueryParser")
def jqlQueryParser = ComponentAccessor.getOSGiComponentInstanceOfType(jqlQueryParserClass)
def nserv = com.atlassian.jira.component.ComponentAccessor.getOSGiComponentInstanceOfType(com.exalate.api.node.INodeService.class)
def remoteIssueUrn = replica.customFields."TS Ticket"?.value

// This will check if there is a remote issue URN so we can do a replace otherwise you do a replace on a null object which can cause errors.
if(remoteIssueUrn){
    remoteIssueUrn = remoteIssueUrn.replaceAll("s","")
}

//debug.error(remoteIssueUrn.toString())

if(remoteIssueUrn && firstSync){ // a TS Ticket is provided, so link to that issue (if you find it)
    def query = jqlQueryParser.parseQuery("key = ${replica.customFields."TS Ticket"?.value}")
    def search = searchService.search(nserv.proxyUser, query, PagerFilter.getUnlimitedFilter()) 
    
    if(search.results.size() != 1) {return}
    else{
        issue.id = search.results.get(0).id
    //debug.error("first sync - id ${issue.id}")
    //TS Ticket result found so connect the issues
        issue.projectKey = "TS"
        issue.typeName = "Production Support"
        issue.customFields."TX Ticket".value = replica.key
    }
    syncHelper.syncBackAfterProcessing() 
}

if(!remoteIssueUrn && firstSync){ 
    //No TS Ticket provided so we're creating a TS Ticket
    issue.projectKey = "TS"
    issue.typeName = replica."TS Issue Type".value?.value
    issue.summary = replica.summary
    issue.description = replica.description //Required to create the TS ticket
    issue.customFields."TaxSys Queue".value = replica.customFields."TaxSys Queue"?.value?.value
    issue.customFields."Domain".value = replica.customFields."Domain"?.value?.value
    //Priority
    issue.priority = replica.priority
    //People data
    issue.assignee = nodeHelper.getUserByEmail(replica.assignee?.email)
    issue.reporter = nodeHelper.getUserByEmail(replica.assignee?.email)
    //The TX Ticket is required on the TS ticket for remote linking - when unlinking
    issue.customFields."TX Ticket".value = replica.key
    //Syncing Fields
    issue.customFields."Release Notes".value = replica.customFields."Release Notes"?.value
    issue.customFields."Secondary Domain".value = replica.customFields."Secondary Domain"?.value?.value

    issue.customFields."TaxSys Activity".value = replica.customFields."TaxSys Activity"?.value?.value
    issue.customFields."County".value = replica.customFields."County"?.value?.value
    issue.customFields."Page".value = replica.customFields."Page"?.value
    syncHelper.syncBackAfterProcessing() 
}