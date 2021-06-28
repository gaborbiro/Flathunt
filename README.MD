### How to build and run:

Just run the `jar` gradle command. Hitting enter on the jar file should open the CLI.

### How to use:

Welcome to Flathunt!

This app does not use any spareroom.co.uk API, but actually opens the website in a browser and reads data by "scraping"
it.

It has two main modes of operating. It can scan your inbox for incoming messages with the 'inbox' command. It fetches
all the properties mentioned in the messages, checks if they match your search criteria and saves them for later
viewing.

You can also create a search profile on spareroom.co.uk, grab the url and pass it to the 'search' command. The command
will similarly parse all properties and validate them.

Alternatively, you can grab a property's ID (it's usually an 8 digit number if the property url but it's also visible on
the property webpage itself as "Ad ref#").

Properties that are not a good match will be marked as unsuitable on the website and therefore won't show up in any
future searches. You can still view these properties by going to the "Saved ads"/"Room to rent ads" section of the
website and selecting "Unsuitable ads" from the Show: filter.

If your started with the 'inbox' command, messages might also be labeled as 'Rejected'/'Partially rejected' if all/some
properties found in them are unsuitable. Check out Tag.kt for other labels used by the app. Note: these are currently
hardcoded. If you change the login account, you will need to create these labels yourself and update
Tag.kt with the html id's.

Check out Setup.kt for validation criteria and other settings needed by this app.