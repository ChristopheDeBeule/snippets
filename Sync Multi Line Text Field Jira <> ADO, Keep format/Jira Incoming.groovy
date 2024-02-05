import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.safety.Whitelist
import org.jsoup.nodes.Node

// Start HtmlToWiki Class
class HtmlToWiki {
    /**
     * This map contains the tranlation between an html tag and a wiki tag.
     * Note that some tags require a start and stop (like in the case of bold tags
     * If ADO sends over a text field it sends it over with HTML tags note that after every new line is a div tag.
     * So we map the div tag to a \n 
     */
    static Map<String, List<String>> tagMap = [
        
        //  html tag : [ <starttag>, <stoptag> ]
        "h1"    : ["h1. ", "\n"],
        "h2"    : ["h2. ", "\n"],
        "h3"    : ["h3. ", "\n"],
        "h4"    : ["h4. ", "\n"],
        "h5"    : ["h5. ", "\n"],
        "h6"    : ["h6. ", "\n"],
        "i"     : ["_", "_"],
        "u"     : ["", ""],
        "strike": ["", ""],
        "em"    : ["_", "_"],
        "b"     : ["*", "*"],
        "strong": ["*", "*"],
        "p"     : ["", "\n"],
        "br"    : ["", ""],
        "hr"    : ["", "-----"],
        "table" : ["", ""],
        "tbody" : ["", ""],
        "div"   : ["", "\n"],
        "tr"    : ["", ""],
        "td"    : ["", ""],
        "th"    : ["", ""],
        "pre"   : ["{noformat}","{noformat}"],
        "code"  : ["{code}","{code}"]
    ]
    
    // the dummy is to meet the jsoup requirement that relative URL's can only be resolved if a basic URL is provided
    // Check https://github.com/jhy/jsoup/issues/1484#issuecomment-770073048 for more details
    static private String DUMMY = "http://dummy/"
    
    
    private int listLevel = 0  // keep track of indentations
    private boolean ignoreCodeTag = false // code tags in pre formatted clause must be ignored
    private Map<String, String> imageNames = [:]
    private Whitelist safeList
    private Integer imgWidth
    
    
    // Constructor is empty
    
    HtmlToWiki() {
        makeSafeList()
        
    }
    
    
    /**
     * if the attachments are provided, the src attribute will be replaced with the proper local filename
     *
     * @param imageAttachments
     */
    
    HtmlToWiki(List imageAttachments) {
        makeSafeList()
        
        imageAttachments.each {
            imageNames.put(it.remoteIdStr as String, it.filename as String)
        }
        
    }
    
    
    /**
     * Whenever an image tag is added a thumbnail modifier is included as in
     *    !^<name>|thumbnail!
     *
     * In case that the imageWidth is set then the modifier will be
     *    !^<anem>|width=<provided parameter>!
     *
     * @param imageWidth
     */
    void setImgWidth ( Integer imageWidth) {
        this.imgWidth = imageWidth
    }
    
     String formatString(String htmlText) {
        if (!htmlText || htmlText == "")
            return ""
        
        Document doc = Jsoup.parse(cleanup(htmlText))
        if (doc == null) {
            throw new Exception("Euh - I really can't parse ${htmlText} the parser returns a null")
        }
        Elements bodyChildren = doc.body().childNodes()
        
        return process(bodyChildren)
    }
    
    /**
     * Ensure that only allowable tags are processed, and avoid any type of xss
     *
     * @return
     */
    private makeSafeList() {
        // Whitelist is deprated in 1.14.1 but that jar is not available today (210129)
        safeList = Whitelist.basicWithImages()
        
        // add all supported tags to the safelist
        String[] fullTagList = tagMap.keySet().toArray(new String[tagMap.size()])
        
        safeList
            .addTags(fullTagList)
            .preserveRelativeLinks(true)
            .addAttributes("span", "style")
        
    }
    
    /**
     * Remove everything which is not processed by jira wiki notation
     * And clean out the text from unsafe html. Only allow the Safelist
     * https://jsoup.org/apidocs/org/jsoup/safety/Safelist.html
     * a, b, blockquote, br, caption, cite, code, col, colgroup, dd, div, dl, dt, em,
     * h1, h2, h3, h4, h5, h6, i, img, li, ol, p, pre, q, small, span, strike, strong,
     * sub, sup, table, tbody, td, tfoot, th, thead, tr, u, ul, img
     *
     * Also hr
     */
    
    private String cleanup(String htmlText) {
        Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        
        htmlText = Jsoup.clean(htmlText, DUMMY, safeList, outputSettings)
        return htmlText
    }
    
    /**
     *
     * @param clauses
     * @return
     */
    private String process(Elements clauses) {
        String result = ""
        
        clauses.each {
            clause ->
                result += process(clause)
        }
        
        return result
    }
    
    
    /**
     * Processing a single element can be broken down to either process an img, a, ol, ul or any other tag
     * which can contain multiple elements
     *
     * @param clause
     * @return processed string in wiki format
     */
    private String process(Element clause) {
        String tagName = clause.tagName()
        
        switch (tagName) {
            case "img":
                return processImage(clause)
            
            case "a":
                return processHref(clause)
            
            case "ol":
                return processList(clause, "#")
            
            case "ul":
                return processList(clause, "*")
            
            case "span":
                return processSpan(clause)
            
            case "pre":
                return processPre(clause)
            case "table":
                return processTable(clause)
            case "code":
                if (ignoreCodeTag) {
                    return processChilds(clause)
                }
        }
        
        return startTag(tagName) + processChilds(clause) + stopTag(tagName)
    }
    
    
    /**
     * This method processes a list (either ol or ul) and transforms it accordingly.  Each of the line item
     * can contain clauses again
     *
     * @param clause - the full list
     * @param listItemMarkUp - the list item marker
     * @return processed string in wiki format
     */
    
    private String processList(Element clause, String listItemMarkUp) {
        
        // Increase the depth of the list, allowing to increase the number of listItemMarkups
        listLevel++
        // all the childnodes should have <li> tags
        
        
        String result = clause
            .childNodes()
            .inject("") { r, node ->
                if (node instanceof Element && node.tagName() == "li") {
                    def lineBreakOrNothingIfLast = node.nextSibling() == null ? "" : "\n"
                    r += (listItemMarkUp * listLevel) + " " + processChilds((Element) node) + lineBreakOrNothingIfLast
                } else if (node instanceof TextNode) {
                    r += node.wholeText
                            .replaceAll("\n", "")
                            .replaceAll("\r", "")
                }
                r
            }

        listLevel--
        return result
    }
    
    
    /**
     * A single line item can contain clauses or sublists and so on
     *
     * @param clause - the list item
     * @param listItemMarkUp - the list item marker
     * @return processed string in wiki format
     */
    private String processListItem(Element clause, String listItemMarkUp) {
        // if there is a list item (starting with the 'li' tag, repeat the markup chars listLevel times
        
        
        if (clause.tagName() == "li")
            return (listItemMarkUp * listLevel) + " " + processChilds(clause) + "\n"
        else return ""
        
    }
    
    private String processChilds(Element clause) {
        String result = clause
            .childNodes()
            .inject("") { r, node ->
                r += process(node)
                r
            }
        
        return result
        
    }
    
    
    /**
     * A single line item can also just contain an entry
     *
     * @param textNode - the text entry
     * @param ignore
     * @return processed string in wiki format
     */
    
    private String processListItem(TextNode textNode, String ignore) {
        return textNode.getWholeText()
    }
    
    
    /**
     * Process an anchor in the form <a href="ref"/> or <a href="ref">label</a>
     * The label can also contain clauses
     *
     * @param clause
     * @return processed string in wiki format
     */
    
    private String processHref(Element clause) {
        String href = clause.attr("href")
        String result = ""
        
        clause.childNodes().each {
            child -> result += process(child)
        }
        
        
        return result > "" ? "[${result}|${href}]" : "[${href}]"
    }
    
    
    /**
     * Process an img in the form <img src="ref"/> or <a href="ref">label</a
     
     *
     * @param clause
     * @return processed string in wiki format
     */
    private String processImage(Element clause) {
        
        
        if (clause.attr("title") == "database image")
        // ignore images retrieved from database in servicenow are ignored
            return ""
        
        
        String sourceName = clause.attr("src")
        
        if (!sourceName)
            return ""
        
        
        if (sourceName.contains("sys_attachment.do") && !imageNames.isEmpty()) {
            // servicenow method to refer a file is sys_attachment.do?sys_id=<a number>
            // a number is a sysid which gets mapped to the local filename
            
            def matcher = sourceName =~ /sys_attachment.do\?sys_id=(\S+)/
            if (matcher.hasGroup()) {
                String sys_id = matcher[0][1] as String
                sourceName = imageNames[sys_id]
            }
        }
        
        if (sourceName.contains("/_apis/wit/attachments/")) {
            // azure devops has an api which starts with wit/attachments to retrieve the attachment
            // the file name (which is also the local file name, is the fileName=<a name> parameter
            
            def matcher = sourceName =~ /fileName=(\S+)/
            if (matcher.hasGroup()) {
                sourceName = matcher[0][1] as String
            }
        }
        
        return imgWidth ? " !^${sourceName}|width=${imgWidth}!" : " !^${sourceName}|thumbnail!"
    }
    
    
    /**
     * Process a span element - currently only supporting color
     */
    
    private String processSpan(Element clause) {
        String styleAttribute = clause.attr("style")
        
        
        String result = ""
        clause.childNodes().each {
            child -> result += process(child)
        }
        
        result = processColorHash(styleAttribute, result)
        result = processColorRGB(styleAttribute, result)
        return result
    }
    
    /*
    ** processPre - code tags should be ignored
     */
    private String processPre(Element clause) {
        ignoreCodeTag = true
        String result = startTag("pre") + processChilds(clause) + stopTag("pre")
        ignoreCodeTag = false
        return result
    }

    /**
     * This method processes a table and transforms it accordingly.  Each of the rows
     * can contain clauses again
     *
     * @param clause - the full table
     * @return processed string in wiki format
     */
    private String processTable(Element clause) {
        def result
        List<Element> trs = clause
            .childNodes()
            .inject([] as List<Element>, collectTrs)

        result = trs
            .collect { Element tr ->
                def thAndTds = tr
                    .childNodes()
                    .inject([] as List<Element>, collectThsAndTds)
                def trStr = thAndTds
                        .inject("") { str, thOrTd ->
                            def prefix = thOrTd.tagName() == "th" ? "||" : "|"
                            def thOrTdBody= process(thOrTd)
                            def wrapped = (thOrTdBody.contains("\n")) ? "{panel}"+ thOrTdBody +"{panel}" : thOrTdBody
                            return str + prefix + wrapped
                        }
                return trStr + "|"
            }
            .join("\n")

        return result
    }
    private Closure<List<Element>> collectTrs
    {
        collectTrs = { List<Element> trs, Node e ->
            if (e instanceof Element && ((Element)e).tagName() == "tr") {
                trs.add((Element)e)
                return trs
            } else {
                return e
                    .childNodes()
                    .inject(trs, collectTrs)
            }
        }
    }
    private Closure<List<Element>> collectThsAndTds
    {
        collectThsAndTds = { List<Element> thAndTds, Node e ->
            if (e instanceof Element && (((Element)e).tagName() == "th" || ((Element)e).tagName() == "td")) {
                thAndTds.add((Element)e)
                return thAndTds
            } else {
                return e
                    .childNodes()
                    .inject(thAndTds, collectThsAndTds)
            }
        }
    }

    /*
    ** Convert a color style specifying the color as a hash
     */
    private String processColorHash(String styleAttribute, String result) {
        def colorMatcher = styleAttribute =~ /color: #(\S+);/
        if (!colorMatcher || !colorMatcher.hasGroup())
            return result
        
        return "{color:#${colorMatcher[0][1]}}${result}{color}"
        
    }
    
    /*
    ** Convert a color style specifying the color as a rgb such as color: rgb(123,234,56)
     */
    
    private String processColorRGB(String styleAttribute, String result) {
        def colorMatcher = styleAttribute =~ /(\S+):\s*rgb\(\s*(\d+),\s*(\d+),\s*(\d+)\);/
        if (!colorMatcher || !colorMatcher.hasGroup())
            return result
        
        def match = colorMatcher.find {
            it[1] == "color"
        }
        if (!match) return result

        try {
            def red = match[2].toInteger()
            def green = match[3].toInteger()
            def blue = match[4].toInteger()
            String hexColor = String.format("#%02x%02x%02x", red, green, blue)
            return "{color:${hexColor}}${result}{color}"
        } catch (Exception e) {
            // there is some problem converting the numbers or calculating the hexformat, or the pattern is missing stuff
            
            return result
        }
        
        
    }
    
    private String startTag(String tagName) {
        return tagMap[tagName] ? tagMap[tagName].get(0) : "?${tagName}?"
    }
    
    private String stopTag(String tagName) {
        return tagMap[tagName] ? tagMap[tagName].get(1) : "?${tagName}?"
    }
    
    /**
     * A text node doesn't contain any subnodes
     *
     * @param text
     * @return the text as is
     */
    
    private String process(TextNode text) {
        return text.getWholeText()
    }
    
}
// End HtmlToWiki Class

if(firstSync){
   issue.projectKey   = "DEMO" 
   // Set type name from source issue, if not found set a default
   issue.typeName     = nodeHelper.getIssueType(replica.type?.name, issue.projectKey)?.name ?: "Task"
}
issue.summary      = replica.summary
issue.description  = replica.description
issue.comments     = commentHelper.mergeComments(issue, replica)
issue.attachments  = attachmentHelper.mergeAttachments(issue, replica)
issue.labels       = replica.labels

// This is to format a customField (Multi line field)
HtmlToWiki htw = new HtmlToWiki()
def DemoScriptCF = replica.customFields."Demo Script".value ?: " "

issue.customFields."Multi text".value = htw.formatString(DemoScriptCF)

