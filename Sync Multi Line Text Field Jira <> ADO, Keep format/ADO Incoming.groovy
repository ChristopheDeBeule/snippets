if(firstSync){
   // Set type name from source entity, if not found set a default
   workItem.projectKey  =  "Christophe"
   workItem.typeName = nodeHelper.getIssueType(replica.type?.name)?.name ?: "Task";
}

workItem.summary      = replica.summary
workItem.description  = replica.description
workItem.attachments  = attachmentHelper.mergeAttachments(workItem, replica)
workItem.comments     = commentHelper.mergeComments(workItem, replica)
workItem.labels       = replica.labels
workItem.priority     = replica.priority

def multiTextCF = replica.customFields."Multi text"?.value ?: " "
multiTextCF = multiTextCF.replace("\n", "<br>");
workItem.customFields."Demo Script"?.value = multiTextCF