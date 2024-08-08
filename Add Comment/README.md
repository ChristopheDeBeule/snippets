# Add comment 


This script will add a comment, only when the comment does not exist already.



By default the comment will be internal, the script provided will set the comment public.

you can also change the comment body.



Create a variable with your comment value, this can be hardcoded or remote value

```
// Value from a custom field
String customTextField = replica.customFields."Demo"?.value
```

```
// hardcoded value
String customTextField = "This comment is added one time."
```



Now we will check if the comment is already in the comment list.

```
if (customTextField && !issue.comments.collect{it.body.contains(customTextField)}.contains(true)){
  // Add the comment to the comment list
  // commentHelper.addComment(String "comment you want to add", Boolean 'sync back', List comments)
  issue.comments = commentHelper.addComment(customTextField, false, issue.comments).collect{
    // Only change the added comment values 
    if (it.body.contains(customTextField)){
      it.internal = false
      // If you don't want to change the body you can remove the line below.
      it.body = "Custom Field Comment: ${it.body}"
    }
    it
  }
}
```




Exalate Source: 

1. [commentHelper](https://docs.exalate.com/docs/commenthelper-in-script-helpers)
2. [addComment](https://docs.exalate.com/docs/addcomment-6226057)
