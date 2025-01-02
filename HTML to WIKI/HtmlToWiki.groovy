@Grapes(
    @Grab(group='org.jsoup', module='jsoup', version='1.6.2')
)
import org.jsoup.*
import org.jsoup.nodes.*

String htmlText = """
<h1>Heading one here</h1>
<h2>Heading two here</h2>
<h3>Heading three here</h3>
<h4>Heading four here</h4>
<h5>Heading five here</h5>
<h6>Heading six here</h6>
<br>
<p>just some <strong>bold</strong> text with  link <a href='https://google.com'>to google</a></p>
<p>abc <i>def <b>bold italic</b></i> ghi</p>
<br>

<ul>
  <li>Coffee</li>
  <li>Tea
    <ul>
      <li>Black tea</li>
      <li>Green tea</li>
    </ul>
  </li>
  <li>Milk</li>
</ul>
<br>
<ol>
  <li>Tacos</li>
  <li>Pasta
    <ul>
      <li>spaghetti
        <ol>
           <li> Bolonaise</li>
        </ol>
      </li>
      <li>farfalle</li>
    </ul>
  </li>
</ol>
"""

Document document = Jsoup.parseBodyFragment(htmlText);


String convertHtmlToJiraWiki(Element root, String textPrefix = "") {
    StringBuilder jiraWiki = new StringBuilder();
    for (Node node : root.childNodes()) {
        if (node instanceof TextNode)
            jiraWiki.append(node.toString().trim())
        else if (node instanceof Element)
            jiraWiki.append(convertHtmlElementToJiraWiki((Element)node, textPrefix))
    }
    return jiraWiki.toString();
}

String convertHtmlElementToJiraWiki(Element element, String textPrefix = "") {
    StringBuilder jiraWiki = new StringBuilder();
    String tag = element.tagName();

    if (tag.size() == 2 && tag.startsWith("h")) {
        jiraWiki.append(tag).append(". ").append(convertHtmlToJiraWiki(element)).append("\n");
    } else if (tag == "p") {
        jiraWiki.append(convertHtmlToJiraWiki(element)).append("\n");
    } else if (tag == "br") {
        jiraWiki.append("\n");
    } else if (tag == "i" || tag == "em") {
        jiraWiki.append("_").append(convertHtmlToJiraWiki(element)).append("_");
    } else if (tag == "a") {
        jiraWiki.append("[").append(element.attr("href")).append("|").append(element.text()).append("]");
    } else if (tag == "ol" || tag == "ul") {
        /*
        This loop is for correctly processing nested lists. 
        It makes sure that only the children of a <ul> or <ol> are processed at the current level and that the right prefix is applied based on the type of list.
        prefix being: (* || #)
        */
        for (Element li : element.select("> li")) {
            jiraWiki.append(convertHtmlElementToJiraWiki(li, textPrefix + (tag == "ul" ? "*" : "#")));
        }
    } else if (tag == "li") {
        jiraWiki.append("\n").append(textPrefix).append(" ").append(convertHtmlToJiraWiki(element, textPrefix));
    } else if (tag == "strong" || tag == "b") {
        jiraWiki.append("**").append(convertHtmlToJiraWiki(element)).append("**");
    } else {
        throw new Exception("Unknown tag detected: " + tag);
    }

    return jiraWiki.toString();
}


// Convert HTML to Jira Wiki
String jiraWikiText = convertHtmlToJiraWiki(document.body());
System.out.println(jiraWikiText);

