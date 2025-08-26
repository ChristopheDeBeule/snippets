import org.jsoup.Jsoup
import org.jsoup.nodes.*

// Check if the field has a value
// Needs to be HTML to work.
if(replica.description){
    // Parse the String to HTML fragments
  Document document = Jsoup.parseBodyFragment(replica.description);
  // Look for the part you want to extract/remove
  // This is done via headers
  def header = document.select(":matchesOwn((?i).*Acceptance criteria.*)").first()
  String extractedAcceptanceCriteria = ""
  if (header != null) {
    def nextSibling = header.nextElementSibling()
    def content = new StringBuilder()
    // Add the header aka 'Acceptance criteria' to the new string
    content.append(header) // remove this line if you dont want "Acceptance criteria" in your text field

    while (nextSibling != null && !nextSibling.tagName().matches("h[1-6]")) {
      content.append(nextSibling.outerHtml())
      nextSibling = nextSibling.nextElementSibling()
    }
    
    extractedAcceptanceCriteria = content.toString()
    // If you want to remove only you can ignore the part above^
    // Now we remove the extracted section from the original document:
    // Remove the <h1> tag with "Acceptance criteria" and the following sibling content (ul, p, etc.)
    def sectionToRemove = header
    while (sectionToRemove != null && sectionToRemove != nextSibling) {
      def temp = sectionToRemove.nextElementSibling()
      sectionToRemove.remove()
      sectionToRemove = temp
    }
  }
  // Set the field where you want the extracted part in. 
  workItem."Microsoft.VSTS.TCM.ReproSteps" = extractedAcceptanceCriteria
  // Set the original string without the extracted part.
  workItem.description = document.outerHtml()
}