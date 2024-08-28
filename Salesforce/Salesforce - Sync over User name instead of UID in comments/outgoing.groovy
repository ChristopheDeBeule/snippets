replica.comments       = entity.comments
  replica.comments.each{
    def user = httpClient.get("/services/data/v58.0/query/?q=SELECT+Id,+Username+FROM+User+WHERE+Id='${it.author.key}'")?.records?.Username[0]
    it.author.key = user
    it
  }