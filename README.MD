### How to run:

> java -jar FlatHunt-rightmove-exp.jar

Check the available executable jars in the project folder to see what services are available.
You can run the jar with a one-off command by specifying the -c argument, with the actual command in quote marks.

Alternatively run <projetc folder>\src\main\java\app\gaborbiro\flathunt\FlatHunt.kt#main() with one of the service configurations passed as VM option (for ex: -DserviceConfig=idealista-exp). See services.gradle for current service configurations. Or the generated Constants.kt file.

Sometimes websites are iffy about being scraped. FlatHunt does its best to log in, persist cookies and re-use them between sessions. If a website still doesn't want to work, disable auto-login, authenticate in your regular browser (FlatHunt uses special chrome instances) and "steal" the cookies. I recommend this chrome plugin: https://chrome.google.com/webstore/search/editthiscookie. My usual routine is to open the root website, launch the plugin, do a "Delete all" in order to clean out old cookies, then log in and export the fresh cookies (select json as export method in the plugin's settings). Paste your cookies in cookie-override.txt and use the "import cookies" command. This will discard old cookies from the app database and use your (hopefully) better ones. Trial and error might be required.

Tip: sometimes "clear browser cookies" is not enough to reset a website. Go to Chrome's History and clear all history manually.

### How to build:

Check what driver version you need by looking at your current Chrome install's version and fetch the right chromedriver.exe from https://chromedriver.chromium.org/downloads.
Note: you need a Stable release of Chrome. Beta/Dev/Canary won't work.

Create a local.properties file and add google.api.key=[your Google API key]. 
Google services needed: Directions API, Maps Static API (both billable).
Check API key restrictions here: https://console.cloud.google.com/apis/credentials/key/67a81ac4-2d5d-4b61-b971-713a6ccbdc67?project=carbon-inkwell-321308

Run .\gradlew allJars

#### How to add a new service:
- Add the name of the new service to services.gradle. This name will be combined with all available criteria-set names (see also services.gradle) and a runtime variable will be generated.
- Use this new runtime variable (Constants.kt) with a koin @Named annotation on your new Service. Check existing services to see how to do it. Convenient runnable jar files are built for all your service-criteria combinations.

#### How to add a new criteria-set:
- Add the name of the new criteria-set to services.gradle. This name will be combined with all available services names (see also services.gradle) and runtime variables will be generated.
- In <project folder>\src\main\java\app\gaborbiro\flathunt\di\di.kt#getValidationCriteria(String) add entries for all the new service configuration names that contain your new criteria-set.

### How to use:

Welcome to Flathunt!

This app does not use any flathunt website APIs but uses web-scraping.
It has two main modes of operating. 

It can scan your inbox (spareroom.co.uk only) for incoming messages with the 'inbox' command. 
It fetches all the properties mentioned in messages.

The other way to use it is to search for properties. Create a search url on spareroom/rightmove/zoopla/idealista and pass it to the 'search' command. You also fetch individual properties by id/url.

In all cases, properties are validated against the criteria-set (each jar has one criteria-set), invalid ones are discarded and mark as unsuitable on the respective website. Valid ones are saved.
Note: you can usually still see the "hidden" properties in the respective website.
 
Once you have a database of suitable properties, you can manage it. Use the 'open' command to view a property and use 'tyn' to discard it and move to the next one.

If your started with the 'inbox' command (spareroom), messages might also be labeled as 'Rejected'/'Partially rejected' 
if all/some properties found in them are unsuitable. Check out Tag.kt for other labels used by the app. 
Note: these are currently hardcoded. If you change the login account, you will need to create these labels yourself and update
Tag.kt with the html id's.

Check out Criteria.kt for validation criteria and other settings used by this app.