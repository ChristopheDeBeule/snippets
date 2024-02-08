// Tempo
// To map the user to the right one, set default user when not found.
def defaultUser = "christophe.debeule@exalate.com"
def wLogUserMap = [
        "user1":"user2"
]

// This will add one or more days to your given time
Date addOneDayToTimestamp(Timestamp timestamp, Integer daysToAdd) {
    Calendar calendar = Calendar.getInstance()
    calendar.setTimeInMillis(timestamp.getTime())
    // Add day(s) to your given time
    calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
    
    return calendar.getTime()
}
// note that we add 1 extra day because Tempo add the logs -1 day 
issue.workLogs = workLogHelper.mergeWorkLogs(issue, replica, {w ->
    w.author = nodeHelper.getUserByEmail(wLogUserMap[w.author?.email] ?: defaultUser)
    w.startDate = addOneDayToTimestamp(w.startDate, 1) // Only add positive numbers 
})