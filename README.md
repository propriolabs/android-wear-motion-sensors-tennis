# ProprioCollect

Copyright 2016 Proprio Labs

## Installation

#### Clone the following repositories

	git clone https://github.com/proprioadmin/proprio-android-wear
	git clone https://github.com/mattgroh/proprio-motion-classification
	git clone https://github.com/proprioadmin/proprio-data-server

#### Install ElasticSearch

	brew install elasticsearch17

### Install Android Studio

Download Android Studio at http://developer.android.com/sdk/index.html. Next, update all SDK tools in Android Studio

### Specify server path

For local development, find your inet localhost configuration with the following line in the terminal

	ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1'

Modify proprioapp/mobile/src/main/res/values/strings.xml and change `data_server` and `analysis_server` and enter your inet localhost configuration.

### Credentials 

Make sure to update credentials in the strings.xml folder in the mobile and wear app. This includes Fabric, Twitter, Facebook, and Open Weather Maps

## Syncing Wirelessly Connected Watches

Load ProprioApp to phone/watch from Android Studio. If you're having trouble with loading the app onto the Android wear use the following two commands
  
    adb forward tcp:4444 localabstract:/adb-hub
  
    adb connect localhost:4444
