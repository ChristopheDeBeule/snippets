import groovy.json.JsonOutput 


// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

/**
 * Builds an ADF mention node for a given accountId.
 */
Map mention(String accountId) {
  if (!accountId) return null
  return [
    type : "mention",
    attrs: [
      id  : accountId,
      text: "@"
    ]
  ]
}

/**
 * Parses a comment body string and converts it to an ADF doc.
 * Handles [~accountid:xxx] tokens inline — in order — replacing them
 * with mention nodes (if the user exists locally) or plain "@accountid" text.
 */
Map textToAdf(String body) {
  if (!body) return null

  List contentList = []

  def parts = body.split(/(?=\[~accountid:[^\]]+\])|(?<=\[~accountid:[^\]]+\])/)

  parts.each { part ->
    def mentionMatch = part =~ /^\[~accountid:([a-zA-Z0-9:_\-]+)\]$/

    if (mentionMatch.matches()) {
      String remoteAccountId = mentionMatch.group(1)
      String localAccountId  = nodeHelper.getUser(remoteAccountId)?.key

      if (localAccountId) {
        contentList << mention(localAccountId)
      } else {
        contentList << [type: "text", text: "@${remoteAccountId}"]
      }
    } else if (part) {
      contentList << [type: "text", text: part]
    }
  }

  return [
    type   : "doc",
    version: 1,
    content: [
      [
        type   : "paragraph",
        content: contentList
      ]
    ]
  ]
}

/**
 * Builds the visibility block for internal comments.
 * JSM  → sd.public.comment property
 * JS   → visibility role (role name must exist in your instance)
 *
 * @param internal        true = internal/restricted comment
 * @param projectTypeKey  "service_desk" for JSM, "software" for Jira Software
 */
Map buildVisibility(boolean internal, String projectTypeKey) {
  if (!internal) return [:]

  if (projectTypeKey == "service_desk") {
    return [
      properties: [
        [
          key  : "sd.public.comment",
          value: [internal: true]
        ]
      ]
    ]
  }

  if (projectTypeKey == "software") {
    return [
      visibility: [
        type : "role",
        value: "Service Desk Team" // adjust to match your instance role name
      ]
    ]
  }

  return [:]
}

/**
 * Creates and registers an Exalate trace for a synced comment.
 *
 * @param localId   the newly created comment id on this side
 * @param remoteId  the original comment id from the replica
 */
void createTrace(String localId, String remoteId) {
  if (!localId || !remoteId) {
    log.warn("createTrace: skipping — localId=${localId}, remoteId=${remoteId}")
    return
  }

  def trace = new com.exalate.basic.domain.BasicNonPersistentTrace()
    .setType(com.exalate.api.domain.twintrace.TraceType.COMMENT)
    .setToSynchronize(true)
    .setLocalId(localId)
    .setRemoteId(remoteId)
    .setAction(com.exalate.api.domain.twintrace.TraceAction.NONE)

  traces.add(trace)
}

// ─────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────

String projectTypeKey = issue.projectTypeKey ?: "software"

if (projectTypeKey == "service_desk") {
  // Service desk does not have comment threads so we can add the comments as is.
  issue.comments     = commentHelper.mergeComments(issue, replica)
}else if (projectTypeKey == "software") {

  Map parentOldIdToNewId = [:]
  // ── Pass 1: parent comments (Jira Software) ──
  replica.addedComments
    .findAll { it.role == null }
    .each { comment ->
      Map body = [
        body: textToAdf(comment.body as String)
      ] + buildVisibility(comment.internal as boolean, projectTypeKey)

      def tmpRes = httpClient.post(
        "/rest/api/3/issue/${issue.key}/comment",
        JsonOutput.toJson(body)
      )

      if (tmpRes?.id) {
        parentOldIdToNewId[comment.remoteId as String] = tmpRes.id
        createTrace(tmpRes.id as String, comment.remoteId as String)
      } else {
        log.warn("addedComments pass 1: no id returned for remoteId=${comment.remoteId}")
      }
    }

  // ── Pass 2: reply comments — Jira Software only ──
  replica.addedComments
    .findAll { it.role != null }
    .each { comment ->
      String parentId = parentOldIdToNewId[comment.role as String]
        ?: traces.find { it.remoteId == comment.role }?.localId

      if (!parentId) {
        log.warn("addedComments pass 2: could not resolve parentId for remoteId=${comment.remoteId}, role=${comment.role} — skipping")
        return
      }

      Map body = [
        body    : textToAdf(comment.body as String),
        parentId: parentId
      ] + buildVisibility(comment.internal as boolean, projectTypeKey)

      def tmpRes = httpClient.post(
        "/rest/api/3/issue/${issue.key}/comment",
        JsonOutput.toJson(body)
      )

      if (tmpRes?.id) {
        createTrace(tmpRes.id as String, comment.remoteId as String)
      } else {
        log.warn("addedComments pass 2: no id returned for remoteId=${comment.remoteId}")
      }
    }
}
