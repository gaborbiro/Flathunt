### How to run:

#### With one of the runnable jars:
> java -jar Flathunt-rightmove-exp.jar

You can run the jar like this, or alternatively with a one-off command by specifying the -c argument, with the actual command in quote marks.
<br>Run "help" first to see what command are available.

#### With Intellij IDEA:
The main class is _**\src\main\java\app\gaborbiro\flathunt\Flathunt.kt**_
<br> Note: you need to pass a service configuration as VM option. For ex: 
> -DserviceConfig=idealista-exp

This app uses chromedriver to launch an instance of your computer's Chrome. Their version must match and if they don't, you will see an exception 
(try running _root_ to test the driver).
<br>Check what version you got installed (you need stable release of Chrome, beta/dev/canary won't work), 
if needed fetch the right chromedriver.exe from https://googlechromelabs.github.io/chrome-for-testing/ and replace the one in the project folder.

To see what service configurations are available check _**\services.gradle**_. 
A service config is always a service name and a validation criteria name separated by a dash.

Sometimes websites are iffy about being scraped. Flathunt does its best to log in, persist cookies and re-use them between sessions. 
If a website still doesn't want to work, disable auto-login.
<br>Log in to the website in your regular browser (anti-bot services can detect Selenium) and "steal" the cookies. 
<br>I recommend this plugin (if you use Chrome): https://chrome.google.com/webstore/search/editthiscookie. 
The app's Chrome instance always has it installed.
<br>Figuring out the cookies might require some trial and error. 
Once you manage to have a session, run _save cookies_ in order to retain the session for next time you run the app.
In my experience it stays valid for the day.
<br>Other useful commands: _clear browser cookies_, _clear stored cookies_.
Tip: sometimes "clear browser cookies" is not enough to reset a website. Go to the Chrome instance's History and clear it manually.

### How to build:

> .\gradlew allJars

Create a local.properties file and add google.api.key=[your Google API key]. 
Google services needed: Directions API, Maps Static API (both billable).
<br>https://console.cloud.google.com/apis/credentials/key/67a81ac4-2d5d-4b61-b971-713a6ccbdc67?project=carbon-inkwell-321308

#### How to add a new service:
- Add the name of the new service to services.gradle. This name will be combined with all available criteria-set names (see also services.gradle) and a runtime variable will be generated.
- Use this new runtime variable (Constants.kt) with a koin @Named annotation on your new Service. Check existing services to see how to do it. Convenient runnable jar files are automatically built for all your service-criteria combinations.

#### How to add a new criteria-set:
- Add the name of the new criteria-set to services.gradle. This name will be combined with all available services names (see also services.gradle) and runtime variables will be generated.
- In <project folder>\src\main\java\app\gaborbiro\flathunt\di\di.kt#getValidationCriteria(String) add entries for all the new service configuration names that contain your new criteria-set.

### How to use:

Welcome to Flathunt!

This app does not use any flathunt APIs. Rather it does web-scraping.

It has two main modes of operating. 

It can scan your inbox (spareroom.co.uk only) for incoming messages with the 'inbox' command. 
It fetches all the properties mentioned in messages.

The other way to use it is to search for properties. Create a search url on spareroom or rightmove or zoopla or idealista and pass it to the 'search' command. You also fetch individual properties by id/url.

In all cases, properties are validated against the criteria-set (each jar has one criteria-set), invalid ones are discarded and mark as unsuitable on the respective website. Valid ones are saved.
Note: you can usually still see the "hidden" properties in the respective website.
 
Once you have a database of suitable properties, you can manage it. Use the 'open' command to view a property and use 'tyn' to discard it and move to the next one.

If your started with the 'inbox' command (spareroom), messages might also be labeled as 'Rejected'/'Partially rejected' 
if all/some properties found in them are unsuitable. Check out Tag.kt for other labels used by the app. 
Note: these are currently hardcoded. If you change the login account, you will need to create these labels yourself and update
Tag.kt with the html id's.

Check out Criteria.kt for validation criteria and other settings used by this app.