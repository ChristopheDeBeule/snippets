// import these modules
import java.text.SimpleDateFormat
import java.util.Date

// Function to convert date time
def convertJiraTimeToAdoTime(String dateString){
    // Define the input and output date formats
    String inputFormat = "yyyy-MM-dd HH:mm:ss.S"
    String outputFormat = "EEE MMM dd HH:mm:ss 'UTC' yyyy"

    // Create SimpleDateFormat objects for the input and output formats
    SimpleDateFormat inputDateFormat = new SimpleDateFormat(inputFormat)
    SimpleDateFormat outputDateFormat = new SimpleDateFormat(outputFormat)

    // Parse the input date string into a Date object
    Date date = inputDateFormat.parse(dateString)
    
    // Convert the Date object into the output format
    String adoDateString =  outputDateFormat.format(date) // String
    def dateFormat = new SimpleDateFormat(outputFormat)
    return dateFormat.parse(adoDateString)
}

// Define the input date string
String inputDateString = replica.customFields."Start date"?.value.toString()
// set the date field in ado equal to the function this will return a class java.util.Date type
workItem.customFields."Date".value = convertJiraTimeToAdoTime(inputDateString)