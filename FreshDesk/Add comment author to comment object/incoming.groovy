replica.comments = replica.comments.collect{
    def res = httpClient.get("/api/v2/agents/${it.author?.key}")?.contact
    if(res){
        it.author.displayName = res.name 
        it.author.email = res.email
    }
    it
}
