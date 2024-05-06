class GroovyHttpClient {
    // SCALA HELPERS
    private static <T> T await(scala.concurrent.Future<T> f) { scala.concurrent.Await$.MODULE$.result(f, scala.concurrent.duration.Duration$.MODULE$.Inf()) }
    private static <T> T orNull(scala.Option<T> opt) { opt.isDefined() ? opt.get() : null }
    private static <T> scala.Option<T> none() { scala.Option$.MODULE$.<T>empty() }
    @SuppressWarnings("GroovyUnusedDeclaration")
    private static <T> scala.Option<T> none(Class<T> evidence) { scala.Option$.MODULE$.<T>empty() }
    private static <L, R> scala.Tuple2<L, R> pair(L l, R r) { scala.Tuple2$.MODULE$.<L, R>apply(l, r) }
 
    // SERVICES AND EXALATE API
    private httpClient
 
    def parseQueryString = { String string ->
        string.split('&').collectEntries{ param ->
            param.split('=', 2).collect{ URLDecoder.decode(it, 'UTF-8') }
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
                throw new com.exalate.api.exception.IssueTrackerException("Parsing of URI failed: $uri\n$e", e)
            }
            parsedUri
        }
    }
 
    GroovyHttpClient(httpClient) {
        this.httpClient = httpClient
    }
 
    String http(String method, String url, String body, java.util.Map<String, List<String>> headers) {
        http(method, url, body, headers) { Response response ->
            if (response.code >= 300) {
                throw new com.exalate.api.exception.IssueTrackerException("Failed to perform the request $method $url (status ${response.code}), and body was: \n```$body```\nPlease contact Exalate Support: ".toString() + response.body)
            }
            response.body as String
        }
    }
 
    public <R> R http(String method, String _url, String body, java.util.Map<String, List<String>> headers, Closure<R> transformResponseFn) {
        def unsanitizedUrl = _url
        def parsedUri = parseUri(unsanitizedUrl)
 
        def embeddedQueryParams = parsedUri.queryMap
 
        def allQueryParams = embeddedQueryParams instanceof java.util.Map ?
                ({
                    def m = [:] as java.util.Map<String, List<String>>;
                    m.putAll(embeddedQueryParams.collectEntries { k, v -> [k, [v]] } as java.util.Map<String, List<String>>)
                    m
                })()
                : ([:] as java.util.Map<String, List<String>>)
 
        def urlWithoutQueryParams = { String url ->
            URI uri = new URI(url)
            new URI(uri.getScheme(),
                    uri.getUserInfo(), uri.getHost(), uri.getPort(),
                    uri.getPath(),
                    null, // Ignore the query part of the input url
                    uri.getFragment()).toString()
        }
        def sanitizedUrl = urlWithoutQueryParams(unsanitizedUrl)
 
        def response
        try {
            def request = ({
            try { httpClient.azureClient }
            catch (e) { httpClient.issueTrackerClient } 
            })()
            .ws
            .url(sanitizedUrl)
            .withMethod(method)
 
            if (headers != null && !headers?.isEmpty()) {
                def scalaHeaders = scala.collection.JavaConverters.asScalaBuffer(
                        headers?.entrySet().inject([] as List<scala.Tuple2<String, String>>) { List<scala.Tuple2<String, String>> result, kv ->
                            kv.value.each { v -> result.add(pair(kv.key, v) as scala.Tuple2<String, String>) }
                            result
                        }
                )
                request = request.withHeaders(scalaHeaders.toSeq())
            }
 
            if (!allQueryParams?.isEmpty()) {
                def scalaQueryParams = scala.collection.JavaConverters.asScalaBuffer(allQueryParams?.entrySet().inject([] as List<scala.Tuple2<String, String>>) { List<scala.Tuple2<String, String>> result, kv ->
                    kv.value.each { v -> result.add(pair(kv.key, v) as scala.Tuple2<String, String>) }
                    result
                })
                request = request.withQueryString(scalaQueryParams.toSeq())
            }
 
            if (body != null) {
                def writable = play.api.http.Writeable$.MODULE$.wString(play.api.mvc.Codec.utf_8())
                request = request.withBody(body, writable)
            }
 
            response = await(
                    request.execute()
            )
        } catch (Exception e) {
            throw new com.exalate.api.exception.IssueTrackerException("Unable to perform the request $method $_url with body: \n```$body```\n, please contact Exalate Support: ".toString() + e.message, e)
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

// Get time tracking

// Note when you create a new token it can take a few moments to do API calls. when you create a new token it's possible the first 30 min you'll get an authentication error.
// Get API Key from env Vars
def apiKey = System.getenv('API_KEY')

def res = ""
try{
    res = new GroovyHttpClient(httpClient)
    .http(
            "GET",
            "https://christophedebeule.timehub.7pace.com/api/odata/v3.2/workLogsOnly",
            null,
            ["Accept": ["application/json"], "Content-type" : ["application/json"], "Authorization":["Bearer <Your 7pace token>"] ]
    )
    {
        response ->
        if (response.code >= 400) {
            throw new com.exalate.api.exception.IssueTrackerException("Failed to get orgnaizations with name ${response.body}")
        } else response.body as String
    }
}catch (e){
    throw new Exception("Error occurred: ${e.message}")
}

def js = new groovy.json.JsonSlurper()
def json = js.parseText(res)
replica.customKeys."7pace" = [:]
for(int i = 0; i < json.value.size();i++){
    if (json.value[i].WorkItemId.toString() == workItem.key.toString()){
        replica.customKeys."7pace".put("PeriodLength",json.value[i]?.PeriodLength)
        replica.customKeys."7pace".put("Comment",json.value[i]?.Comment)
        replica.customKeys."7pace".put("WorklogDate",[
            "ShortDate": json.value[i]?.WorklogDate?.ShortDate,
            "Year": json.value[i]?.WorklogDate?.Year,
            "Month": json.value[i]?.WorklogDate?.Month,
            "Day": json.value[i]?.WorklogDate?.Day
        ])
        replica.customKeys."7pace".put("User",[
            "Name": json.value[i]?.User?.Name,
            "Email": json.value[i]?.User?.Email,
        ])
        replica.customKeys."7pace".put("EditedByUser",[
            "Name": json.value[i]?.EditedByUser?.Name,
            "Email": json.value[i]?.EditedByUser?.Email,
        ])
    }
    
}

/*


This is how it would look in the outgoing sync:
 "customKeys": {
      "7pace": {
        "PeriodLength": 3600,
        "Comment": "Test one hour",
        "WorklogDate": {
          "ShortDate": "2024-05-06",
          "Year": 2024,
          "Month": 5,
          "Day": 6
        },
        "User": {
          "Name": "John Doe",
          "Email": "john.doe@test.com"
        },
        "EditedByUser": {
          "Name": "John Doe",
          "Email": "john.doe@test.com"
        }
      }
    },


Values: 

"value": [
        {
            "Id": "0a0a0000-0a0a-000a-a0a0-00000a0000aa",
            "UserId": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
            "AddedByUserId": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
            "BudgetId": null,
            "ActivityTypeId": "00000000-0000-0000-0000-000000000000",
            "WorkItemId": 442,
            "Timestamp": "2024-04-25T15:56:00Z",
            "PeriodLength": 3600,
            "Comment": "Testing",
            "EditedTimestamp": "2024-04-25T22:56:18.993Z",
            "CreatedTimestamp": "2024-04-25T22:56:18.993Z",
            "IsTracked": false,
            "IsManuallyEntered": true,
            "IsChanged": false,
            "IsTrackedExtended": false,
            "IsImported": false,
            "IsFromApi": true,
            "IsBillable": false,
            "BillablePeriodLength": null,
            "AddedByUser": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "John Doe",
                "Email": "john.doe@test.com"
            },
            "ActivityType": {
                "Id": "00000000-0000-0000-0000-000000000000",
                "Color": "#",
                "Name": "[Not Set]"
            },
            "User": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "John Doe",
                "Email": "john.doe@test.com"
            },
            "EditedByUser": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "John Doe",
                "Email": "john.doe@test.com"
            },
            "Budget": null,
            "WorklogDate": {
                "Year": 2024,
                "Month": 4,
                "Day": 25,
                "ShortDate": "2024-04-25"
            }
        },
        {
            "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
            "UserId": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
            "AddedByUserId": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
            "BudgetId": null,
            "ActivityTypeId": "00000000-0000-0000-0000-000000000000",
            "WorkItemId": 449,
            "Timestamp": "2024-05-06T13:34:00Z",
            "PeriodLength": 3600,
            "Comment": "Test one hour",
            "EditedTimestamp": "2024-05-06T20:34:58.55Z",
            "CreatedTimestamp": "2024-05-06T20:34:58.55Z",
            "IsTracked": false,
            "IsManuallyEntered": true,
            "IsChanged": false,
            "IsTrackedExtended": false,
            "IsImported": false,
            "IsFromApi": true,
            "IsBillable": false,
            "BillablePeriodLength": null,
            "AddedByUser": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "John Doe",
                "Email": "john.doe@test.com"
            },
            "ActivityType": {
                "Id": "00000000-0000-0000-0000-000000000000",
                "Color": "#",
                "Name": "[Not Set]"
            },
            "User": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "John Doe",
                "Email": "john.doe@test.com"
            },
            "EditedByUser": {
                "Id": "0a0aaaa0-0000-0000-a0a0-a00000000aaa",
                "Name": "",
                "Email": "john.doe@test.com"
            },
            "Budget": null,
            "WorklogDate": {
                "Year": 2024,
                "Month": 5,
                "Day": 6,
                "ShortDate": "2024-05-06"
            }
        }
    ]
*/