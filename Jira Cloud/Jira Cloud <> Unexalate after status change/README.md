# Un Exalate your sync after Status update.


In Jira cloud you can add a [post function](https://support.atlassian.com/jira-cloud-administration/docs/configure-advanced-issue-workflows/#Post-functions) to your workflow.


Please add the Un-Exalate post function to your specific transition.

In other connectors, you cannot add this post function to un-exalate your sync.

To accomplish this, you need to sync from the other instance and create an automation to change the status. This will trigger the transition and un-exalate the sync.

Please refer to the demo script provided in this repository.


### Steps

* Check the scripts and comments within.
* Create a Jira automation that will check if your specific custom field is updated.
  1. The automation should have a check on the field update.
  2. It should update the transition to the transition you set your post function to.


source: [how-to-stop-issue-sync-in-jira-cloud](https://docs.exalate.com/docs/how-to-stop-issue-sync-in-jira-cloud)
