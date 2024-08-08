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

    def listType = matches.group(1) == "*" ? "ul" : "ol"
    return new lineProcessResult(i, "<${listType}>${listItems.join()}</${listType}>")
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
    def regex = /\[(.*?)\s*\|\s*(http?:\/\/[^\s\]]+)\]/
    def matches = line =~ regex

    //Check if the pattern found a match 
    if (!matches.find()) {
      // When no match is found we return the Original line if the isList is true otherwise we return an empty String
      return isList ? line : ""
    }

    def tmpLine = line.replace(matches.group(0), "<a href=\"${matches.group(2)}\">${matches.group(1)}</a>").trim()
    // We add a line break if it's not a list item, list items don't need a line break.

    if(isList)
      return tmpLine
    
    return "${tmpLine}<br>"
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


  public def wikiToHTML(String wiki){
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
        text += splitted[index] + "<br>" 
      else
        text += appender

      index++
    }
    // This will set the color if there are color atributes.
    text = text.replaceAll(/\{color:#([0-9a-fA-F]{6})\}(.*?)\{color\}/, "<span style=\"color:#\$1\">\$2</span>")
    return text
  }
}

// on the first sync its possible the user mention will not be set correctly
// Therfore we need to create the new workItem first and set the description, comment or your string.

if(firstSync){
  issue.projectKey = "FOO"
  issue.typeName = "Task"
  issue.summary = "Hello there"
  store(issue) // creates the issue with only project, type and summary
}

// Create a new WikiToHtml obect and call the wikiToHTML method.
WikiToHtml convert = new WikiToHtml()
def convertedString = convert.wikiToHTML(yourString)
