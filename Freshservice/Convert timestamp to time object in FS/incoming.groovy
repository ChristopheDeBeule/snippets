import java.sql.Timestamp

String convertTimeStamp(Timestamp ts) {
    def ldt = ts.toLocalDateTime().withNano(0)
    String tmp = java.time.format.DateTimeFormatter
             .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
             .format(ldt)
    return tmp+"-04:00" // If we don't add the -04:00 the day will be moved up one
    // 25th => 24th 
    // e.g. "2025-10-25 00:00:00"
}

// Demo date is the dataType Timestamp.
String tmp = convertTimeStamp(replica.customFields."Demo Date"?.value)
entity.planned_start_date = tmp