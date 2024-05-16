import groovy.json.JsonSlurper
import java.time.Duration

def extractValues(map, keys) {
    def extracted = [:]
    keys.each { key ->
        def parts = key.split('\\.')
        def value = map
        parts.each { part ->
            if (value instanceof Map && value.containsKey(part)) {
                value = value[part]
            } else {
                value = null
                return
            }
        }
        if (value != null) {
            extracted[key.replaceAll('\\.', '')] = value
        }
    }
    return extracted
}

// This will convert the time (seconds) to a pretty format
def prettyPrintTime(seconds){
    Duration duration =  Duration.ofSeconds(seconds.toInteger())
    def hours = duration.toHours()
    def minutes = duration.minusHours(hours).toMinutes()

    return (hours > 0 ? hours + "h " : "") + (minutes > 0 ? minutes + "m" : "")
}

def extractAndFormatValues(customField) {
    // Create a json object to read from
    def jsonSlurper = new JsonSlurper()
    def list = jsonSlurper.parseText(customField)
    // Keys to extract 
    def keysToExtract = [
        'PeriodLength',
        'Comment',
        'WorklogDate.ShortDate',
        'EditedByUser.Name'
    ]
    // We will use this to rename the key values 
    def renameMap = [
        'PeriodLength':"Time logged",
        'Comment':"Comment",
        'WorklogDateShortDate':"Time logged on",
        'EditedByUserName':"Time logged by"

    ]

    def extractedValues = list.collect { item ->
        extractValues(item, keysToExtract)
    }
    // To find the total time
    def totalTimeSpent = list."Total Time Spent"[-1].toString() // This will get the last value of totalTime Spent
    def outputString = extractedValues.collect { item ->
        item.collect { k, v -> 
            if (renameMap[k] == "Time logged"){
                "${renameMap[k]}: ${prettyPrintTime(v)}" 
            }else{
                "${renameMap[k]}: ${v}" 
            }
                
        }.join('\n')
    }.join('\n\n')

    if (totalTimeSpent != null) {
        outputString += "\nTotalTimeSpent: ${prettyPrintTime(totalTimeSpent)}"
    }
    return outputString
}

issue.customFields."CF Name".value = extractAndFormatValues(replica.customKeys."7pace")

// Usage per instance
/*
    issue.customFields."CF Name".value = extractAndFormatValues(replica.customKeys."7pace")
    entity.CF_Name = extractAndFormatValues(replica.customKeys."7pace")
    workItem.customFields."CF Name".value = extractAndFormatValues(replica.customKeys."7pace")
*/