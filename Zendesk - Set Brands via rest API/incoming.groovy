def brand = replica.customFields."CF Name"?.value?.value

def brands = [
  // JIRA Value: Brand ID (can be found in the admin center)
  // "CF Value":"Brand ID" 
  "Exalate":"17220150238226" // Example 
]

// This will set the brads in the ZD Ticket
httpClient.put("/api/v2/tickets/${issue.key}", "{\"ticket\":{\"brand_id\":${brands[brand]}}}")
