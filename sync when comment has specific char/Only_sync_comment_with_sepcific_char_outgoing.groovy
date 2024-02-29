// Example to sync comments with specific char SF outgoing
// have this one to trigger the new comments
replica.comments = entity.comments
// Method one
def list = []
for(def i = 0; i < entity.comments.size(); i++){
def comment = entity.comments[i]
    if(comment.body.toString().toLowerCase().contains("#jira")){
        list += entity.comments[i]
    }
}
replica.comments = list