// TODO: Script Bugs when a sentence is written in italics and contains bold text.

class lineProcessResult{
  int index
  String value
    
  lineProcessResult(int index, String value) {
    this.index = index
    this.value = value
  }
}

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
      
      listItems += "<li>${match.group(2)}</li>"
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

def processBoldText(String line) {
  def regex = /\*(.*?)\*/ /* /^\s*\*(.*)\*\s*$/ */
  def matches = line =~ regex
  
  if(!matches.find())
    return ""
  
  String tmp = "<strong>${matches.group(1)}</strong>"
  return line.replaceAll("\\*${matches.group(1)}\\*",tmp)
   
}

def processItalicText(String line) {
  def regex = /_(.*?)_/      /* /^\s*_(.*)_\s*$/ */
  def matches = line =~ regex

  if(!matches.find())
    return ""
  
  String tmp = "<em>${matches.group(1)}</em>"
  return line.replaceAll("_${matches.group(1)}_",tmp)
}

def wikiToHtml(String wiki){
  splitted = wiki.split(System.lineSeparator())
  String text = ""
  
  int index = 0
  
  while(index < splitted.size()){
    def lineResult = processList(splitted, index)
    index = lineResult.index
    def appender = lineResult.value
    appender += processHeader(splitted[index])
    appender += processBoldText(splitted[index])
    appender += processItalicText(splitted[index])
    
    if(appender == "")
      text += splitted[index] + "<br>" 
    else
      text += appender
    index++
  }

  // Separate handling for links to ensure they match the correct format
  text = text.replaceAll(/\[([^\[\]|]+)\|([^\[\]]+)\]/) { match, urlText, url ->
    if (url.startsWith("http")) {
      "<a href=\"${url}\">${urlText}</a>"
    } else {
      match.group(0) // return the original text if it's not a proper URL
    }
  }   
  return text
}

if(firstSync){
  entity.entityType = "Case"
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  //debug.error(html)
  entity.Multi_text__c  = wikiToHtml(replica.description)

  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.comments     = commentHelper.mergeComments(entity, replica)
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)
}