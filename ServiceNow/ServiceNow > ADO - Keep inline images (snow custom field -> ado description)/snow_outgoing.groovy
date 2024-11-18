if (entity.tableName == "incident") {
    replica.key            = entity.key
    replica.summary        = entity.short_description
    replica.description    = entity.description
    replica.attachments    = entity.attachments
    replica.comments       = entity.comments
    replica.state          = entity.state
    replica.u_html_1 = entity.u_html_1 // your customField 
}