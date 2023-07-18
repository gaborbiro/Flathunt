### How to run:

> java -jar FlatHunt-rightmove-exp.jar

Note: you can't run FlatHunt just like on its own, it needs a service specified. A service is basically a combination of the flathunting website you want to use and a criteria set.
Currently, three services are supported: rightmove-exp, spareroom-exp and zoopla-exp. "exp" being the filtering criteria used to find the right flat. See Validation.kt#EXP

Running ".\gradlew allJars" creates an executable jar file for all three services.

Alternatively run app/gaborbiro/flathunt/FlatHunt.kt#main() with one of the services passed as argument.

### How to build:

Check what driver version you need by looking at your current Chrome install's version and fetching the corresponding chromedriver.exe from https://chromedriver.chromium.org/downloads
Note: you need a Stable release of Chrome. Beta/Dev/Canary won't work.

Create a local.properties file and add google.api.key=[your Google API key]. 
Google services needed: Directions API, Maps Static API (both billable).

Run .\gradlew allJars

### How to use:

Welcome to Flathunt!

This app does not use any flathunt website APIs but uses web-scraping.
It has two main modes of operating. 

It can scan your inbox (if the website supports it) for incoming messages with the 'inbox' command. 
It fetches all the properties mentioned in the messages, checks if they match your search criteria and saves them for later viewing. (Only supported for spareroom.co.uk).

You can also create a search profile on spareroom/rightmove/zoopla, grab the url and pass it to the 'search' command (make sure you use the jar that corresponds to the website). The command will similarly parse all properties and validate them.

Alternatively, you can grab a property's ID (it's usually an 8 digit number in the property url but it's also visible on the property webpage itself as "Ad ref#").

Properties that are not a good match will be marked as unsuitable on the website and therefore won't show up in any future searches. 
On spareroom, you can still view these properties by going to the "Saved ads"/"Room to rent ads" section of the website and selecting "Unsuitable ads" from the Show: filter.

If your started with the 'inbox' command (spareroom), messages might also be labeled as 'Rejected'/'Partially rejected' 
if all/some properties found in them are unsuitable. Check out Tag.kt for other labels used by the app. 
Note: these are currently hardcoded. If you change the login account, you will need to create these labels yourself and update
Tag.kt with the html id's.

Check out Setup.kt for validation criteria and other settings used by this app.