import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.sql.Timestamp

if(firstSync){
  entity.entityType = "Case"
}


def convertJiraDate(date) {
  // Check if the date is a java.sql.Timestamp object
  if (date instanceof Timestamp) {
    // Convert java.sql.Timestamp to milliseconds
    date = date.getTime()
  }

  // Ensure the date is a long
  if (!(date instanceof Long)) {
    throw new Exception("Date must be a java.sql.Timestamp or Long representing milliseconds since epoch")
  }

  // Convert timestamp in milliseconds to Instant
  def instant = Instant.ofEpochMilli(date)

  // Format Instant to ISO 8601 date string format
  def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC)
  def dateString = formatter.format(instant)
  
  return dateString
}

if(entity.entityType == "Case"){
  
    // Set your custom date fields 
    entity.Custom_date_time__c = convertJiraDate(replica.customFields."Custom Date Time"?.value) // Date/Time
    entity.Custom_date__c = convertJiraDate(replica.customFields."Custom Date"?.value) // Date field
}


