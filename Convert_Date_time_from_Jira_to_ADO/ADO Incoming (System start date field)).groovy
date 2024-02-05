// import these modules
import java.text.SimpleDateFormat
import java.util.Date

// Define the input date string

def convertJiraTimeToAdoTime(String dateString){
    if(dateString == null) return 
    // Define the input and output date formats
    String inputFormat = "yyyy-MM-dd HH:mm:ss.S"
    String outputFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    // Create SimpleDateFormat objects for the input and output formats
    SimpleDateFormat inputDateFormat = new SimpleDateFormat(inputFormat)
    SimpleDateFormat outputDateFormat = new SimpleDateFormat(outputFormat)

    // Parse the input date string into a Date object
    Date date = inputDateFormat.parse(dateString)
    
    // Convert the Date object into the output format
    return outputDateFormat.format(date) // String
}

// does not set the field
String inputDateString = replica.customFields."Start date"?.value // 1705536000000
workItem."Microsoft.VSTS.Scheduling.StartDate" = convertJiraTimeToAdoTime(inputDateString)