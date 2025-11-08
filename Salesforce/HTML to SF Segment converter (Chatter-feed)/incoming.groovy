import groovy.json.JsonOutput
import org.jsoup.*
import org.jsoup.nodes.*


if(firstSync){
  entity.entityType = "Case"
}

List convertHtmlElementToSegments(Element element) {
  List segments = []

  String tag = element.tagName()

  // Paragraph <p>
  if (tag == "p") {
      segments << ["type": "MarkupBegin", "markupType": "Paragraph"]
      element.childNodes().each { child ->
          if (child instanceof Element) {
              segments += convertHtmlElementToSegments(child) // Recursively process child elements
          } else {
              segments << ["type": "Text", "text": child.text()]
          }
      }
      segments << ["type": "MarkupEnd", "markupType": "Paragraph"]
  }
  // Bold <strong> or <b>
  else if (tag == "strong" || tag == "b") {
      segments << ["type": "MarkupBegin", "markupType": "Bold"]
      element.childNodes().each { child ->
          if (child instanceof Element) {
              segments += convertHtmlElementToSegments(child) // Recursively process child elements
          } else {
              segments << ["type": "Text", "text": child.text()]
          }
      }
      segments << ["type": "MarkupEnd", "markupType": "Bold"]
  }
  // Italic <i> or <em>
  else if (tag == "i" || tag == "em") {
      segments << ["type": "MarkupBegin", "markupType": "Italic"]
      element.childNodes().each { child ->
          if (child instanceof Element) {
              segments += convertHtmlElementToSegments(child) // Recursively process child elements
          } else {
              segments << ["type": "Text", "text": child.text()]
          }
      }
      segments << ["type": "MarkupEnd", "markupType": "Italic"]
  }
  // Hyperlink <a>
  else if (tag == "a") {
    segments << ["type": "MarkupBegin", "markupType": "Hyperlink", "url": element.attr("href")]
    element.childNodes().each { child ->
        if (child instanceof Element) {
            segments += convertHtmlElementToSegments(child) // Recursively process child elements
        } else {
            segments << ["type": "Text", "text": child.text()]
        }
    }
    segments << ["type": "MarkupEnd", "markupType": "Hyperlink"]
  }
  // Unordered list <ul>
  else if (tag == "ul") {
      segments << ["type": "MarkupBegin", "markupType": "UnorderedList"]
      element.select("> li").each { li ->
          segments += convertHtmlElementToSegments(li) // Recursively process list items
      }
      segments << ["type": "MarkupEnd", "markupType": "UnorderedList"]
  }
  // Ordered list <ol>
  else if (tag == "ol") {
      segments << ["type": "MarkupBegin", "markupType": "OrderedList"]
      element.select("> li").each { li ->
          segments += convertHtmlElementToSegments(li) // Recursively process list items
      }
      segments << ["type": "MarkupEnd", "markupType": "OrderedList"]
  }
  // List item <li>
  else if (tag == "li") {
      segments << ["type": "MarkupBegin", "markupType": "ListItem"]
      element.childNodes().each { child ->
          if (child instanceof Element) {
              segments += convertHtmlElementToSegments(child) // Recursively process child elements
          } else {
              segments << ["type": "Text", "text": child.text()]
          }
      }
      segments << ["type": "MarkupEnd", "markupType": "ListItem"]
  }
  // For all other tags, treat them as plain text
  else {
    segments << ["type": "MarkupBegin", "markupType": "Paragraph"]
    element.childNodes().each { child ->
        if (child instanceof Element) {
            segments += convertHtmlElementToSegments(child) // Recursively process child elements
        } else {
            segments << ["type": "Text", "text": child.text()]
        }
    }
    segments << ["type": "MarkupEnd", "markupType": "Paragraph"]
  }
  
  return segments 
}

if(entity.entityType == "Case"){
  entity.Subject      = replica.summary
  entity.Description  = replica.description
  entity.Origin       = "Web"
  entity.Status       = "New"
  entity.attachments  = attachmentHelper.mergeAttachments(entity, replica)

    if (!firstSync){
        replica.addedComments.collect {
            String SFCommentId = ""
            // Replace <ins> & </ins> otherwise its handeled as an unknown tag
            String tmpBody = it.body.replaceAll("<ins>","").replaceAll("</ins>", "")
            def document = Jsoup.parse(tmpBody) 
            List segments = convertHtmlElementToSegments(document.body())
            // Comment body
            Map body = [
                "feedElementType": "FeedItem",
                "subjectId": "${entity.key}",
                "body": [
                    "messageSegments": segments
                ]
            ]
            def res = httpClient.http(
            "POST",
            "/services/data/v54.0/chatter/feed-elements",
            JsonOutput.toJson(body),
            null,
            ['Content-Type': ['application/json']]
            ) {
                req ,
                res ->
                // When the rest api call is successfully fulfilled set the SFCommentId var.
                if (res.code == 201) {

                    SFCommentId = res?.body?.id
                } else {
                    throw new Exception("error while creating comment:" + res.code + " message:" + res.body)
                }
            }
            if ( SFCommentId ) {
            def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
                    .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
                    .setToSynchronize(true)
                    .setLocalId(SFCommentId as String)
                    .setRemoteId(it.remoteId as String)
                    .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)
            traces.add(trace)
            }
        }
    }
}