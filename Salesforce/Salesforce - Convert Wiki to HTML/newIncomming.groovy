class lineProcessResult{
  int index
  String value
    
  lineProcessResult(int index, String value) {
    this.index = index
    this.value = value
  }
}

class WikiToHtml{
  private def processList(def lines, int index) {
    def regex = /^\s*([#*]) \s*(.*)$/
    def matches = lines[index] =~ regex

    if(!matches.find())
      return new lineProcessResult(index, "")

    def i = index
    def listItems = []
    while(i < lines.size()) {
        def match = lines[i] =~ regex
        if(!match.find())
        break

        def tmp = match.group(2)
        tmp = processBoldAndItalicText(tmp)
        tmp = processUrl(tmp, true)
        listItems += "<li>${tmp}</li>"
        i++
    }

    def listType = "ol"
    if(matches.group(1) == "*")
      listType = "ul"

    return new lineProcessResult(i, "<${listType}>${listItems.join()}</${listType}><br>")
  }

  private def processHeader(String line) {
    def regex = /^\s*h([0-6])\.\s*(.*)$/
    def matches = line =~ regex

    if(!matches.find())
      return ""

    return "<h${matches.group(1)}>${matches.group(2)}</h${matches.group(1)}><br>"
  }

  private def processUrl(String line, Boolean isList){
    // Separate handling for links to ensure they match the correct format
    def regex = /\[(.*?)\s*\|\s*(.*?)\]/ /* /\[([^\[\]|]+)\|([^\[\]]+)\]/ */
    def matches = line =~ regex
    //Check if the pattern found a match 
    if (!matches.find()) {
      // When no match is found we return the Original line if the isList is true otherwise we return an empty String
      return isList ? line : ""
    }
    // We add a line break if it's not a list item, list items don't need a line break.
    if(matches.group(2).startsWith("http") && isList)
      return "<a href=\"${matches.group(2)}\">${matches.group(1)}</a>"
    else
      return "<a href=\"${matches.group(2)}\">${matches.group(1)}</a><br>"
  }


  // This function will keep the format if you have bold italic text and regular bold/italic text
  private def processBoldAndItalicText(String line) {
    // Process bold text
    line = replaceText(line, /\*(.+?)\*/, '<strong>', '</strong>')
    // Process italic text
    line = replaceText(line, /_(.+?)_/, '<em>', '</em>')

    return line
  }

  private def replaceText(String text, def regex, String startTag, String endTag) {
    def matcher = text =~ regex
    StringBuffer sb = new StringBuffer()
    while (matcher.find()) {
        matcher.appendReplacement(sb, "${startTag}${matcher.group(1)}${endTag}")
    }
    matcher.appendTail(sb)
    return sb.toString()
  }

  private def fetchInfo(input) {
    def pattern = /BasicHubAttachment\{id=([^,]+), remoteId=`([^`]*)`, mimetype=`([^`]*)`, filename=`([^`]*)`, created=`([^`]*)`, filesize=`([^`]*)`, author=`\{ @key : ([^}]*)\}`, thumbnailable=`([^`]*)`, zip='([^']*)'\}/
    def matches = input.findAll(pattern)
  
    def formattedAttachments = matches.collect { match ->
      def matcher = match =~ pattern
      matcher.find()
      [
        id: matcher.group(1).trim(),
        filename: matcher.group(4).trim()
      ]
    }
    return formattedAttachments
  }

  // New Inline image
  private def processImage(String line, def attachments){
    def regex = /(?:!|alt=\")([^|"]+)/
    def matches = line =~ regex
    
    if(!matches.find())
      return ""

    def alt = matches.group(1) // This return the image name
    def allAttachments = fetchInfo(attachments.toString())
    def tmp = ""
    allAttachments.each{attachment ->
      if(attachment.filename == alt){
        tmp = attachment.id
        return
      }
    }
    return "<a href=\"https://exalate-9b-dev-ed.develop.lightning.force.com/lightning/r/ContentDocument/${tmp}/view\">${alt}</a>"
  }

  public def wikiToHTML(String wiki, attachments = null){

    if(!wiki) return ""

    def splitted = wiki.split(System.lineSeparator())
    String text = ""
    int index = 0

    while(index < splitted.size()){
      def lineResult = processList(splitted, index)
      index = lineResult.index
      def appender = lineResult.value
    
      String headerResult = processHeader(splitted[index])
      if(headerResult){
        appender += headerResult
        index++  // Increment the index to skip the header line in the next loop iteration
      }

      String imageResult = processImage(splitted[index], attachments)
      if (imageResult) {
      	appender += imageResult
        splitted[index] = ""
      }

      // Process URLs separately to ensure they don't get duplicated in text output
      String newUrl = processUrl(splitted[index], false)
      if (newUrl){
          appender += newUrl
          index++  // Increment the index to skip the URL line in the next loop iteration
      } else {
          // Only process bold and italic text if the line is not a URL
          appender += processBoldAndItalicText(splitted[index])
      }

      if(appender.isEmpty())
        text += splitted[index] 
      else
        text += appender + "<br>"

    index++
    }

    return text
  }
}


if(firstSync){
  entity.entityType = "Case"
}

WikiToHtml convert = new WikiToHtml()

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary  

  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)
  store(entity) // we store the issue so it created the ID for the attachmentents
  entity.Description  = convert.wikiToHTML(replica.description,  entity.attachments)
  entity.Multi_text__c  = convert.wikiToHTML(replica.description,  entity.attachments)
}