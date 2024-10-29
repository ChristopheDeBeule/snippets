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
      tmp = processUrl(tmp, true)
      tmp = processText(tmp)
      
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
    def regex = /\[(.*?)\s*\|\s*(https?:\/\/[^\s\]]+)\]/
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
    else
      return "${tmpLine}<br>"
  }

  private def darkenColor(hexColor, percentage = 0.4) {
    // Remove the # if it's there
    hexColor = hexColor.replace("#", "")

    // Parse the hex color into RGB components
    def r = Integer.parseInt(hexColor[0..1], 16)
    def g = Integer.parseInt(hexColor[2..3], 16)
    def b = Integer.parseInt(hexColor[4..5], 16)

    // Calculate the new RGB values, darkened by the given percentage
    r = (r * (1 - percentage)) as int
    g = (g * (1 - percentage)) as int
    b = (b * (1 - percentage)) as int

    // Ensure RGB values are within the 0-255 range
    r = Math.max(0, Math.min(255, r))
    g = Math.max(0, Math.min(255, g))
    b = Math.max(0, Math.min(255, b))

    // Convert the RGB values back to a hex string
    def darkenedHexColor = String.format("#%02X%02X%02X", r, g, b)

    return darkenedHexColor
  }

  // This function will keep the format if you have bold italic text and regular bold/italic text
  private def processText(String line) {
    def regex = /\[~accountid:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\]/ 

    // Process bold text
    line = replaceText(line, /\*(.+?)\*/, '<strong>', '</strong>')
    // Process italic text
    line = replaceText(line, /_(.+?)_/, '<em>', '</em>')
    // Process Strike throug (Ignore dates)
    line = replaceText(line, /(?<!\d{4})-(?!\d{2}-\d{2})(.+?)-(?!\d{2}-\d{2})/, '<strike>', '</strike>')
    // Process SubScript 
    line = replaceText(line, /~(.+?)~/, '<sub>', '</sub>')
    // Process Underline 
    line = replaceText(line, /\+(.+?)\+/, '<u>', '</u>')
    // Process SuprtScript 
    line = replaceText(line, /\^(.+?)\^/, '<sup>', '</sup>')
    // Process Code Block 
    line = replaceText(line, /\{noformat\}(.+?)\{noformat\}/, '<pre><code>', '</code></pre>')
    // Process Small Code Block 
    line = replaceText(line, /\{\{(.+?)\}\}/, '<span style="background-color:rgb(230, 230, 230); border-radius: 3px; padding: 2px;">', '</span>')
    // Process status
    line = replaceText(line, /\[(?!\|)(.+?)\]/, '<span style="background-color:#DYNAMIC_BG_COLOR; color: DYNAMIC_COLOR; border-radius: 3px; padding: 2px;">', '</span>')

    def matches = line =~ regex
    if(matches.find()) return "<br>"
    return line
  }

  private def replaceText(String text, def regex, String startTagTemplate, String endTag) {

    if (text.contains('{noformat}'))
      text = text.replaceAll(" newLn ", "<br>")
    // Define the regex pattern to extract the color code
    def colorPattern = /\{color:#([0-9A-Fa-f]{6})\}/
    def colorMatcher = text =~ colorPattern

    // Extract the color code
    String colorCode = colorMatcher ? colorMatcher[0][1] : "FFFFFF" // Default to white if no color found
    
    // Update the start tag with the extracted color code
    String startTag = startTagTemplate.replace('DYNAMIC_BG_COLOR', colorCode).replace('DYNAMIC_COLOR', darkenColor(colorCode))

    def matcher = text =~ regex
    StringBuffer sb = new StringBuffer()

    while (matcher.find()) {
      matcher.appendReplacement(sb, "${startTag}${matcher.group(1)}${endTag}")
    }
    matcher.appendTail(sb)
    return sb.toString()
  }
  private def calculateCellWidths(row) {
    def totalLength = row.sum { it.length() }
    row.collect { cell -> (cell.length() / totalLength) * 100 /1.4 }
  }
  private def isTable(String text) {
    // Regex pattern to check if the text starts with a table structure
    def tablePattern = /^(?:\|\|.*?\|\|$|\|.*?\|$)/
    return text =~ tablePattern
  }

  private def processTable(String line) {
    // Check if the line is a table; return if it's not
    if (!isTable(line)) return ""

    // Split the table text by `tbreak` to separate rows
    def rows = line.split("tbreak")
    def htmlTable = '''<table style="border-collapse: collapse; width: 100%;" border="1"><tbody>'''

    rows.each { row ->
      // Determine if the row is a header or data row based on starting symbols
      def isHeader = row.startsWith("||")
      def cellDelimiter = isHeader ? /\|\|/ : /\|/

      // Remove leading and trailing delimiters, then split by delimiter
      def cells = row.replaceAll(/^(\|\||\|)/, "").replaceAll(/(\|\||\|)$/, "").split(cellDelimiter)
      
      // Calculate dynamic cell widths based on content length
      def widths = calculateCellWidths(cells)

      // Start row
      htmlTable += "<tr>"
      
      // Process each cell and add it to the HTML row
      cells.eachWithIndex { cell, idx ->
        def content = cell.trim() // Clean up cell content
        htmlTable += isHeader
          ? "<td style=\"width: ${widths[idx]}%;\"><strong>${processText(content)}</strong></td>" // Header cell
          : "<td style=\"width: ${widths[idx]}%;\">${processText(content)}</td>"                  // Data cell
      }

      // End row
      htmlTable += "</tr>"
    }

    htmlTable += "</tbody></table>"
    return htmlTable
  }

  private String processUserMention(String line){
    def regex = /\[~accountid:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\|([^\]]+)\]/
    def matches = line =~ regex
    if(!matches.find()) 
      return ""
    
    matches.each {
      def user = this.helper.getUserByEmail(matches.group(1), this.projectName) 
      if(!user){
        line = line.replace(matches.group(0), "@${matches.group(2)}") // 1 = email, 2 = user name
      }else{ 
        line = line.replace(matches.group(0), "<a href=\"#\" data-vss-mention=\"version:2.0,"+user?.key+"\"></a>")
      }
    } 
    return line + "<br>"
  }

  private def splitText(String text) {
    // Code block handling section
    def noformatPattern = /\{noformat\}([\s\S]*?)\{noformat\}/
    def modifiedText = text.replaceAll(noformatPattern) { match ->
        def codeBlock = match[1].replaceAll('\n', ' newLn ')
        return "{noformat}${codeBlock}{noformat}"
    }
    // End of code block handling

    // Split the modified text by line
    def lines = modifiedText.split(System.lineSeparator())
    def modifiedLines = []
    boolean inTable = false

    // Table handling section
    lines.each { line ->
        if (line.startsWith("||")) {
            // Start of a table: set inTable to true
            if (!inTable) {
                inTable = true
            }
            // Replace newline within table rows with tbreak
            modifiedLines << line.replaceAll('\n', 'tbreak')
        } else if (inTable && line.startsWith("|")) {
            // Continuation of table row: append current line to the last table row
            modifiedLines[-1] += 'tbreak' + line
        } else {
            // End of table: reset inTable to false
            inTable = false
            modifiedLines << line
        }
    }
    // End of table handling

    return modifiedLines
  }
  
  public def wikiToHTML(String wiki){
    //throw new Exception(wiki.replaceAll('\n',"<br>"))
    def splitted = splitText(wiki)
    String text = ""
    boolean check = true
    int index = 0
    String headerResult = ""
    String newUrl = ""
    String userMention = ""
    String table = ""

    while(index < splitted.size()){
      def lineResult = processList(splitted, index)
      index = lineResult.index
      def appender = lineResult.value
      if(index != splitted.size())
        headerResult = processHeader(splitted[index])
      if(headerResult){
        appender += headerResult
        index++  // Increment the index to skip the header line in the next loop iteration
      }
      
      // Process URLs separately to ensure they don't get duplicated in text output
      if(index != splitted.size())
        newUrl = processUrl(splitted[index], false)
      if (newUrl){
        appender += newUrl
      } else {
        if(index != splitted.size())
          appender += processText(splitted[index])
      }

      if(index != splitted.size()){
        table = processTable(splitted[index])
      }
      if(table){
        appender = table
      }

      // if(index != splitted.size())
      //   userMention = processUserMention(splitted[index])
      // if (userMention){
      //   appender = appender.replace(appender,userMention)
      // }

      if (appender.isEmpty() && index != splitted.size())
        text += splitted[index] + "<br>" 
      else
        text += appender

      index++
    }
    // This will set the color if there are color atributes. 
    // The CleanUpText function will find any unhandeled wiki tags and the replace will remove it.
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
// If you use the usermentions change the constructor with the project key and helper
WikiToHtml convert = new WikiToHtml("","")
def convertedString = convert.wikiToHTML(yourString)
