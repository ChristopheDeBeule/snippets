class lineProcessResult{
  int index
  String value
    
  lineProcessResult(int index, String value) {
    this.index = index
    this.value = value
  }
}

class WikiToHtml{

  def helper;
  def projectName;

  public def WikiToHtml(def helper, def projectName){
    this.helper = helper;
    this.projectName = projectName;
  }

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
    def regex = /\[~accountid:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\]/ 

    // Process bold text
    line = replaceText(line, /\*(.+?)\*/, '<strong>', '</strong>')
    // Process italic text
    line = replaceText(line, /_(.+?)_/, '<em>', '</em>')
    def matches = line =~ regex
    if(matches.find()) return "<br>"
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

  private String processUserMention(String line){
    def regex = /\[~accountid:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\]/ 
    def matches = line =~ regex
    
    if(!matches.find()) 
      return ""
    matches.each {
      def user = this.helper.getUserByEmail(matches.group(1), this.projectName) 
           
      if(!user){
        line = line.replace(matches.group(0), matches.group(1))
      }else{ 
        line = line.replace(matches.group(0), "<a href=\"#\" data-vss-mention=\"version:2.0,"+user?.key+"\"></a>")
      }
    } 
    
    return line + "<br>"
  }

  public def wikiToHTML(String wiki){
    def splitted = wiki.split(System.lineSeparator())
    String text = ""
    boolean check = true
    int index = 0

    //throw new Exception("${splitted}")
    while(index < splitted.size()){
      def lineResult = processList(splitted, index)
      index = lineResult.index
      def appender = lineResult.value
      
      String headerResult = processHeader(splitted[index])
      if(headerResult){
        appender += headerResult
        index++  // Increment the index to skip the header line in the next loop iteration
      }
      //throw new Exception("List length ${splitted.size()}")
      // Process URLs separately to ensure they don't get duplicated in text output
      String newUrl = processUrl(splitted[index], false)
      if (newUrl){
        appender += newUrl
        index++  // Increment the index to skip the URL line in the next loop iteration
      } else {
        // Only process bold and italic text if the line is not a URL
        appender += processBoldAndItalicText(splitted[index])
      }
      // Make sure we don't go out of bounds
      if(check && index == splitted.size()){
        check = false
        index--
      }
      String userMention = processUserMention(splitted[index])
      if (userMention){
        appender += userMention
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