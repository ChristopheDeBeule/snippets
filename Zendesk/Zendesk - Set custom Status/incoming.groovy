// We need 2 status mappings one to convert the Jira status to the right custom status ID in zendesk (This id can be found in the admin console)
// When we converted the status we do a separate API call to post the status
// When we posted the custom status we need to set the status category as well that's why we need the 2nd mapping.


def customStatusMap = [
  // Jira status: ZD Custom status ID
  "Open":"29225510469779", // Open
  "In Progress":"29225998880531", // Awaiting Action
  "Done":"29343246417811" // Solved as closed
]

def statusMap = [
  // Your ZD status ID: Your zendesk status type
  // Note the ZD status type is one of these: new, open, pending & solved
  "29225510469779":"open",
  "29225998880531":"pending",
  "29343246417811":"solved"
]

def statusName = replica.status?.name
def state = customStatusMap[statusName] ?: "29225510469779" // default value is open

// Here we set the custom status via the right ID
httpClient.put("/api/v2/tickets/${issue.key}", "{\"ticket\": {\"custom_status_id\": ${state}}}")
// To keep the status in the right status type we need to change the type as well
issue.setStatus(statusMap[state])
