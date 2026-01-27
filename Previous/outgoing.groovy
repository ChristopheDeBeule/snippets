// Get the previous sync.
def getPreviousJson = {
    def ttRepo = (syncHelper).twinTraceRepository
    def await = { f -> scala.concurrent.Await$.MODULE$.result(f, scala.concurrent.duration.Duration.apply(1, java.util.concurrent.TimeUnit.MINUTES)) }
    def orNull = { opt -> opt.isEmpty() ? null : opt.get() }
    def ttOptFuture = ttRepo.getTwinTraceByLocalIssueKeyFuture(connection, issueKey)
    def ttOpt = await(ttOptFuture)
    def tt = orNull(ttOpt)

    def lr = tt?.localReplica
    !lr ? null : ({
        def js = new groovy.json.JsonSlurper()
        def previousPayload = js.parseText(lr.payload)
        previousPayload.hubIssue
    })()
}

def previous = getPreviousJson()