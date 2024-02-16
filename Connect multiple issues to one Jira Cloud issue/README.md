# Connect multiple remote issues to one Jira ticket with Exalate

You will need a trigger to automate this process or you can manual sync the ticket to jira with use of the [Exalate sync pannel](https://docs.exalate.com/docs/sync-panel-in-exalate)

This [trigger](https://docs.exalate.com/docs/triggers-in-exalate) depends on which system you're using on the remote side.

#### Possible Triggers

##### [Jira Cloud](https://support.atlassian.com/jira-service-management-cloud/docs/use-advanced-search-with-jira-query-language-jql/)

`project = "your project" and "customField[Short text]" is not EMPTY`

##### [Zendesk](https://support.zendesk.com/hc/en-us/articles/4408882086298-Searching-tickets)

Note that `custom_field_360015212345` needs the ID if your specific custom field => `custom_field_<custom field ID>`

`type:ticket custom_field_360015212345:*`


## How to connect the issues?


When you added your scripts in the right code block (incoming, outgoing) script and you have your triggers in place.

You can create a new ticket in your instance that is connected to your Jira cloud instance and add the Jira issue-key (ABC-1).

When the issue-key is added in your field the trigger will pick it up and send ot over to your Jira cloud instance and it will be connected.

you can add as many tickets you want, but notice that every update you do on the Jira ticket will be visible on your local issue (only fields you sync back from Jira to your local issue).


You can find a video [here](./Multi_ZD_to_One_Jira_Issue.mov) where I showcase this with Zendesk and Jira Cloud.
