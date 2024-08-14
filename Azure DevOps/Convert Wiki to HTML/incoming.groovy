class lineProcessResult{
  int index
  String value
    
  lineProcessResult(int index, String value) {
    this.index = index
    this.value = value
  }
}

class WikiToHtml{

  def nodeHelper;
  def workItem;
  def replica;
  def syncHelper;

  public def WikiToHtml(def nodeHelper, def syncHelper, def workItem, def replica){
    this.nodeHelper = nodeHelper;
    this.syncHelper = syncHelper;
    this.workItem = workItem;
    this.replica = replica;
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

  private def processInlineImage(String line){
    def supportedFileTypes = ['png', 'jpeg', 'jpg', 'webp']
    def regex = /!(.+\|.+)!/
    def matches = line =~ regex
  
    if (!matches.find()) {
      return ""
    }

    def fileName = matches.group(1).split("\\|")[0]
    Boolean isSupported = supportedFileTypes.any {ext ->
      fileName.toLowerCase().endsWith(".${ext}")
    }
    // TODO: find a good way to handle non suported file types
    if (!isSupported){
      return fileName
    }

    def attId = this.workItem.attachments.find{ it.filename?.equals(fileName)}?.idStr
    return "<img src=\"${this.syncHelper.getTrackerUrl()}/${this.workItem.projectKey}/_apis/wit/attachments/${attId}?fileName=${fileName}\" />"
  }

  private def processCodeBlock(String line, Boolean numeric = false) {
    // Regex to find the content between {noformat} tags
    def regex = /\{noformat\}(.+?)\{noformat\}/
    def matches = line =~ regex
    
    if (!matches.find()) {
      return ""
    }
    
    // Get the matched content and replace `newLn` with <br>, then clean up multiple <br> tags
    def content = matches.group(1).replaceAll(" newLn ", "<br>")
    if(numeric){
      // Add numeric lines to the code block (uncomment if you want numbers in your code block)
      def splitLines = content.split('<br>')
      content = ""
      for(int i = 1; i < splitLines.size() +1; i++){
        content += "${i} ${splitLines[i-1]}\n"
      }
    }
    
    // Wrap the numbered content in <code><pre> tags and return
    return "<code><pre>${content}</pre></code>"
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
    def rg2 = /!(.+\|.+)!/
    def mtch = line =~ rg2
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
    // Process Small Code Block 
    line = replaceText(line, /\{\{(.+?)\}\}/, '<span style="background-color:rgb(230, 230, 230); border-radius: 3px; padding: 2px;">', '</span>')
    // Process status
    line = replaceText(line, /\[(?!\|)(.+?)\]/, '<span style="background-color:#DYNAMIC_BG_COLOR; color: DYNAMIC_COLOR; border-radius: 3px; padding: 2px;">', '</span>')

    def matches = line =~ regex
    if(matches.find()) return "<br>"
    return line
  }

  private def replaceText(String text, def regex, String startTagTemplate, String endTag) {
    // This is done for wiki status sync
    // Define the regex pattern to extract the color code
    if (text =~ /!(.+\|.+)!/) return text
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

  private String processUserMention(String line){
    def regex = /\[~accountid:([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,})\|([^\]]+)\]/
    def matches = line =~ regex
    if(!matches.find()) 
      return ""
    
    matches.each {
      def user = this.nodeHelper.getUserByEmail(matches.group(1), this.workItem.projectKey) 
      if(!user){
        line = line.replace(matches.group(0), "@${matches.group(2)}") // 1 = email, 2 = user name
      }else{ 
        line = line.replace(matches.group(0), "<a href=\"#\" data-vss-mention=\"version:2.0,"+user?.key+"\"></a>")
      }
    } 
    return line + "<br>"
  }

  private def splitText(String text) {
    // replace all \n with newLn so we don't change the code block format.
    def pattern = /\{noformat\}([\s\S]*?)\{noformat\}/

    def modifiedText = text.replaceAll(pattern) { match ->
      def codeBlock = match[1].replaceAll('\n', ' newLn ')
      return "{noformat}${codeBlock}{noformat}"
    }
    modifiedText = modifiedText.split(System.lineSeparator())
  }
  
  public def wikiToHTML(String wiki){
    def splitted = splitText(wiki)
    String text = ""
    boolean check = true
    int index = 0
    
    String headerResult = ""
    String newUrl = ""
    String userMention = ""
    String codeBlock = ""
    String image = ""

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
        image = processInlineImage(splitted[index])
      }
      if(image){
        appender = appender.replace(appender, image)
      }

      if(index != splitted.size()){
        codeBlock = processCodeBlock(splitted[index])
      }
      if(codeBlock){
        appender = appender.replace(appender, codeBlock)
      }

      if(index != splitted.size())
        userMention = processUserMention(splitted[index])
      if (userMention){
        appender = appender.replace(appender,userMention)
      }

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

if(firstSync){
   // Set type name from source entity, if not found set a default
  workItem.projectKey  =  "Your ADO project name"
  workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
  workItem.summary      = replica.summary
  store(issue)
}

workItem.summary      = replica.summary
workItem.labels       = replica.labels
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
// Store after the attachments so the attachment Id will be created localy
store(issue)
WikiToHtml convert = new WikiToHtml(nodeHelper, syncHelper, workItem, replica)

workItem.description  = convert.wikiToHTML(replica.description)
workItem.comments     = commentHelper.mergeComments(workItem, replica, {c ->
  c.body = convert.wikiToHTML(c.body)
  c
})