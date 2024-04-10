def formMap = [
  // JIRA Value: Form ID (can be found in the admin center)
  // "CF Value":"Form ID" 
  "Exalate Form":"13484878553884" // Example
]
def form = replica.customFields."CF Name"?.value?.value
// This will set the Forms in the ZD Ticket
httpClient.put("/api/v2/tickets/${issue.key}", "{\"ticket\": {\"ticket_form_id\": ${formMap[form]}}}")
