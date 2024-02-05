// By default tempo sends over the time in seconds.
// Iterates over logged time
def totalTimeInSeconds = 0
for(int i = 0; i < replica.workLogs.size(); i++){
    totalTimeInSeconds += replica.workLogs[i].timeSpent
}
 
// This function makes the time readable.
def convertToReadableTime(long seconds){
    def hour = (int)Math.floor(seconds / 3600)
    seconds %= 3600
    def minutes = (int)Math.floor(seconds / 60)
    if(hour == 0){
        return "${minutes.toString()}m"
    }
    return "${hour.toString()}h:${minutes.toString()}m"
}
// Add the value to your custom field, by calling the "convertToReadableTime" function with the "totalTimeInSeconds" as parameter.
workItem.customFields."Custom field".value = convertToReadableTime(totalTimeInSeconds)
//issue.customFields."Custom field".value = convertToReadableTime(totalTimeInSeconds)
