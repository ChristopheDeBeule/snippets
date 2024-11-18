
if(firstSync){
   // Set type name from source entity, if not found set a default
   workItem.projectKey  =  "Christophe"
   workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
}

workItem.summary      = replica.summary
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority

// Store after the attachments so the attachment Id will be created localy
store(issue)

def imgTag = replica.u_html_1

// Regex to find all occurrences of `sys_id`
def regex = /sys_id(?:&#61;|=|&)([a-f0-9]{32})/
def matcher = (imgTag =~ regex)
def updatedHtml = imgTag

while (matcher.find()) {
  def sysId = matcher.group(1) // Extracted sys_id
  def foundRemoteAtt = replica.attachments.find { it?.remoteIdStr == sysId }
  def foundLocalAtt = workItem.attachments.find { it?.idStr == foundRemoteAtt?.idStr }

  if (foundRemoteAtt && foundLocalAtt) {
    def newImgTag = "<img src=\"${syncHelper.getTrackerUrl()}/${workItem.projectKey}/_apis/wit/attachments/${foundLocalAtt.idStr}?fileName=${foundLocalAtt.filename}\" />"
    // Replace the specific `img` tag containing this `sys_id`
    updatedHtml = updatedHtml.replaceFirst(/<img[^>]*sys_id(?:&#61;|=|&)${sysId}[^>]*>/, newImgTag)
  }
}

workItem.description = updatedHtml