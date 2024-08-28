// ADO CLient Class Start
class AdoClient {
    // SCALA HELPERS
    private static <T> T await(scala.concurrent.Future<T> f) {
        scala.concurrent.Await$.MODULE$.result(f, scala.concurrent.duration.Duration$.MODULE$.Inf())
    }
    private static <T> T orNull(scala.Option<T> opt) { opt.isDefined() ? opt.get() : null }
    private static <T> scala.Option<T> none() { scala.Option$.MODULE$.<T> empty() }
    @SuppressWarnings("GroovyUnusedDeclaration")
    private static <T> scala.Option<T> none(Class<T> evidence) { scala.Option$.MODULE$.<T> empty() }
    private static <L, R> scala.Tuple2<L, R> pair(L l, R r) { scala.Tuple2$.MODULE$.<L, R> apply(l, r) }
    // SERVICES AND EXALATE API
    private static def getGeneralSettings() {
        def gsOptFuture = nodeHelper.azureClient.generalSettingsService.get()
        def gsOpt = await(gsOptFuture)
        def gs = orNull(gsOpt)
        gs
    }
    private static String getIssueTrackerUrl() {
        final def gs = getGeneralSettings()
        def removeTailingSlash = { String str -> str.trim().replace("/+\$", "") }
        final def issueTrackerUrl = removeTailingSlash(gs.issueTrackerUrl)
        issueTrackerUrl
    }
    private httpClient
    private static nodeHelper
    private debug
    def parseQueryString = { String string ->
        string.split('&').collectEntries { param ->
            param.split('=', 2).collect { URLDecoder.decode(it, 'UTF-8') }
        }
    }
    //Usage examples: https://gist.github.com/treyturner/4c0f609677cbab7cef9f
    def parseUri
    {
        parseUri = { String uri ->
            def parsedUri
            try {
                parsedUri = new URI(uri)
                if (parsedUri.scheme == 'mailto') {
                    def schemeSpecificPartList = parsedUri.schemeSpecificPart.split('\\?', 2)
                    def tempMailMap = parseQueryString(schemeSpecificPartList[1])
                    parsedUri.metaClass.mailMap = [
                            recipient: schemeSpecificPartList[0],
                            cc       : tempMailMap.find { it.key.toLowerCase() == 'cc' }.value,
                            bcc      : tempMailMap.find { it.key.toLowerCase() == 'bcc' }.value,
                            subject  : tempMailMap.find { it.key.toLowerCase() == 'subject' }.value,
                            body     : tempMailMap.find { it.key.toLowerCase() == 'body' }.value
                    ]
                }
                if (parsedUri.fragment?.contains('?')) { // handle both fragment and query string
                    parsedUri.metaClass.rawQuery = parsedUri.rawFragment.split('\\?')[1]
                    parsedUri.metaClass.query = parsedUri.fragment.split('\\?')[1]
                    parsedUri.metaClass.rawFragment = parsedUri.rawFragment.split('\\?')[0]
                    parsedUri.metaClass.fragment = parsedUri.fragment.split('\\?')[0]
                }
                if (parsedUri.rawQuery) {
                    parsedUri.metaClass.queryMap = parseQueryString(parsedUri.rawQuery)
                } else {
                    parsedUri.metaClass.queryMap = null
                }
                if (parsedUri.queryMap) {
                    parsedUri.queryMap.keySet().each { key ->
                        def value = parsedUri.queryMap[key]
                        if (value.startsWith('http') || value.startsWith('/')) {
                            parsedUri.queryMap[key] = parseUri(value)
                        }
                    }
                }
            } catch (e) {
                throw new com.exalate.api.exception.IssueTrackerException("Parsing of URI failed: $uri $e ", e)
            }
            parsedUri
        }
    }
    AdoClient(httpClient, nodeHelper, debug) {
        this.httpClient = httpClient
        this.nodeHelper = nodeHelper
        this.debug = debug
    }
    String http(String method, String path, java.util.Map<String, List<String>> queryParams, String body, java.util.Map<String, List<String>> headers) {
        http(method, path, queryParams, body, headers) { Response response ->
            if (response.code >= 300) {
                throw new com.exalate.api.exception.IssueTrackerException(
                        """Failed to perform the request $method $path (status ${response.code}),
and body was: ```$body```
Please contact Exalate Support: """.toString() + response.body
                )
            }
            response.body as String
        }
    }
    public <R> R http(String method, String path, java.util.Map<String, List<String>> queryParams, String body, java.util.Map<String, List<String>> headers, Closure<R> transformResponseFn) {
        def gs = getGeneralSettings()
        def unsanitizedUrl = issueTrackerUrl + path
        def parsedUri = parseUri(unsanitizedUrl)
        def embeddedQueryParams = parsedUri.queryMap
        def allQueryParams = embeddedQueryParams instanceof java.util.Map ?
                ({
                    def m = [:] as java.util.Map<String, List<String>>;
                    m.putAll(embeddedQueryParams as java.util.Map<String, List<String>>)
                    m.putAll(queryParams)
                })()
                : (queryParams ?: [:] as java.util.Map<String, List<String>>)
        def urlWithoutQueryParams = { String url ->
            URI uri = new URI(url)
            new URI(uri.getScheme(),
                    uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(),
                    null, // Ignore the query part of the input url
                    uri.getFragment()).toString()
        }
        def sanitizedUrl = urlWithoutQueryParams(unsanitizedUrl)
        //debug.error("#debug ${sanitizedUrl}")
        def response
        try {
            def request = ({ 
                try { httpClient.azureClient } 
                catch (e) { httpClient.issueTrackerClient }  
              })()
              .ws
              .url(sanitizedUrl)
              .withMethod(method)
            if (!allQueryParams.isEmpty()) {
                def scalaQueryParams = scala.collection.JavaConverters.asScalaBuffer(
                        queryParams
                                .entrySet()
                                .inject([] as List<scala.Tuple2<String, String>>) { List<scala.Tuple2<String, String>> result, kv ->
                                    kv.value.each { v -> result.add(pair(kv.key, v) as scala.Tuple2<String, String>) }
                                    result
                                }
                )
                request = request.withQueryString(scalaQueryParams.toSeq())
            }
            if (headers != null && !headers.isEmpty()) {
                def scalaHeaders = scala.collection.JavaConverters.asScalaBuffer(
                        headers
                                .entrySet()
                                .inject([] as List<scala.Tuple2<String, String>>) { List<scala.Tuple2<String, String>> result, kv ->
                                    kv.value.each { v -> result.add(pair(kv.key, v) as scala.Tuple2<String, String>) }
                                    result
                                }
                )
                request = request.withHeaders(scalaHeaders.toSeq())
            }
            if (body != null) {
                def writable = play.api.libs.ws.WSBodyWritables$.MODULE$.writeableOf_String()
                request = request.withBody(body, writable)
            }
            def credentials = await(httpClient.azureClient.credentials)
            def token = credentials.accessToken
            //debug.error("${play.api.libs.ws.WSAuthScheme$.class.code}")
            request = request.withAuth(token, token, play.api.libs.ws.WSAuthScheme$BASIC$.MODULE$)
            response = await(request.execute())

        } catch (Exception e) {

            throw new com.exalate.api.exception.IssueTrackerException(
                    """Unable to perform the request $method $path with body:```$body```,
                    please contact Exalate Support: """.toString() + e.message,
                    e
            )
        }
        
        java.util.Map<String, List<String>> javaMap = [:]
        for (scala.Tuple2<String, scala.collection.Seq<String>> headerTuple : scala.collection.JavaConverters.bufferAsJavaListConverter(response.allHeaders().toBuffer()).asJava()) {
            def javaList = []
            javaList.addAll(scala.collection.JavaConverters.bufferAsJavaListConverter(headerTuple._2().toBuffer()).asJava())
            javaMap[headerTuple._1()] = javaList
        }
        def javaResponse = new Response(response.body(), new Integer(response.status()), javaMap)
        return transformResponseFn(javaResponse)
    }
    public static class Response {
        final String body
        final Integer code
        final java.util.Map<String, List<String>> headers
        Response(String body, Integer code, java.util.Map<String, List<String>> headers) {
            this.body = body
            this.code = code
            this.headers = headers
        }
    }
} 
// ADO Client Class END
