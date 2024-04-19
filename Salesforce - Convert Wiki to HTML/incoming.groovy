class lineProcessResult{
  int index
  String value
    
  lineProcessResult(int index, String value) {
    this.index = index
    this.value = value
  }
}

class WikiToHtml{
  def processList(def lines, int index) {
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
        listItems += "<li>${tmp}</li>"
        i++
    }

    def listType = "ol"
    if(matches.group(1) == "*")
      listType = "ul"

    return new lineProcessResult(i, "<${listType}>${listItems.join()}</${listType}>")
  }

  def processHeader(String line) {
    def regex = /^\s*h([0-6])\.\s*(.*)$/
    def matches = line =~ regex

    if(!matches.find())
      return ""

    return "<h${matches.group(1)}>${matches.group(2)}</h${matches.group(1)}>"
  }
  // These functions processBoldText & processItalicText will only work if the string or word is bold or italic.
  // Not when a string or word is bold and italic
  // def processBoldText(String line) {
  //   def regex = /\*(.*?)\*/ /* /^\s*\*(.*)\*\s*$/ */
  //   def matches = line =~ regex

  //   if(!matches.find())
  //     return ""

  //   String tmp = "<strong>${matches.group(1)}</strong>"
  //   return line.replaceAll("\\*${matches.group(1)}\\*",tmp)

  // }

  // def processItalicText(String line) {
  //   def regex = /_(.*?)_/      /* /^\s*_(.*)_\s*$/ */
  //   def matches = line =~ regex

  //   if(!matches.find())
  //     return ""

  //   String tmp = "<em>${matches.group(1)}</em>"
  //   return line.replaceAll("_${matches.group(1)}_",tmp)
  // }

  def processUrl(String line){
    // Separate handling for links to ensure they match the correct format
    def regex = /\[(.*?)\s*\|\s*(.*?)\]/ /* /\[([^\[\]|]+)\|([^\[\]]+)\]/ */
    def matches = line =~ regex
    if(!matches.find())
        return ""
    if(matches.group(2).startsWith("http"))
        return "<a href=\"${matches.group(2)}\">${matches.group(1)}</a>"
    else
        return match.group(0)
  }


  // This function will keep the format if you have bold italic text and regular bold/italic text
  def processBoldAndItalicText(String line) {
    // Process bold text
    line = replaceText(line, /\*(.+?)\*/, '<strong>', '</strong>')
    // Process italic text
    line = replaceText(line, /_(.+?)_/, '<em>', '</em>')

    return line
  }

  def replaceText(String text, def regex, String startTag, String endTag) {
    def matcher = text =~ regex
    StringBuffer sb = new StringBuffer()
    while (matcher.find()) {
        matcher.appendReplacement(sb, "${startTag}${matcher.group(1)}${endTag}")
    }
    matcher.appendTail(sb)
    return sb.toString()
  }


  def wikiToHTML(String wiki){
    def splitted = wiki.split(System.lineSeparator())
    String text = ""

    int index = 0

    while(index < splitted.size()){
      def lineResult = processList(splitted, index)
      index = lineResult.index
      def appender = lineResult.value
      appender += processHeader(splitted[index])
      //appender += processBoldText(splitted[index])
      //appender += processItalicText(splitted[index])
      appender += processBoldAndItalicText(splitted[index])
      appender += processUrl(splitted[index])

      if(appender == "")
        text += splitted[index] + "\n" 
      else
        text += appender
      index++
    }

    return text
  }
}

WikiToHtml convert = new WikiToHtml()

if(firstSync){
  entity.entityType = "Case"
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  //debug.error(html)
  entity.Multi_text__c  = convert.wikiToHTML(replica.description)

  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)
}