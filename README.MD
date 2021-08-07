### How to build:

Create a local.properties file and add google.api.key=[your Google API key]. 
Google services needed: Directions API, Maps Static API (both billable).

### How to run:

Just hit enter on one of the jar file.

### How to use:

Welcome to Flathunt!

This app does not use any flathunt website APIs but uses web-scraping.

It has two main modes of operating. It can scan your inbox for incoming messages with the 'inbox' command. It fetches
all the properties mentioned in the messages, checks if they match your search criteria and saves them for later
viewing. (Only supported for spareroom.co.uk).

You can also create a search profile on spareroom/rightmove/zoopla, grab the url and pass it to the 'search' command 
(using the right jar). The command will similarly parse all properties and validate them.

Alternatively, you can grab a property's ID (it's usually an 8 digit number if the property url but it's also visible on
the property webpage itself as "Ad ref#").

Properties that are not a good match will be marked as unsuitable on the website and therefore won't show up in any
future searches. 
On spareroom, you can still view these properties by going to the "Saved ads"/"Room to rent ads" section of the
website and selecting "Unsuitable ads" from the Show: filter.

If your started with the 'inbox' command (spareroom), messages might also be labeled as 'Rejected'/'Partially rejected' 
if all/some properties found in them are unsuitable. Check out Tag.kt for other labels used by the app. 
Note: these are currently hardcoded. If you change the login account, you will need to create these labels yourself and update
Tag.kt with the html id's.

Check out Setup.kt for validation criteria and other settings needed by this app.