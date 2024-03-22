## This will show you how to have inline comments from Jira OnPrem to ADO


#### Jira OnPrem 

In Jira we need to apply some changes in the outgoing script to change wiki to HTML


Check the following code [here](./JiraOnPrem/outgoing.groovy) 



#### Azure DevOps


Jira sends the image over as a attachment we need to get that attachment id to set it in the comment on the right spot.

We use regex for this to find where we need to apply these changes.


Please find the code [here](./AzureDevOps/incoming.groovy) 


[Reference](https://community.exalate.com/display/exacom/Jira+On-Premise+and+Azure+DevOps%3A+How+to+Synchronize+Inline+Images+between+the+Two)
