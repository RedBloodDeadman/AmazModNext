[![latest service](https://img.shields.io/github/release/RedBloodDeadman/AmazModNext.svg?label=latest%20release&style=flat)](https://github.com/RedBloodDeadman/AmazModNext/releases/latest)

Here is a repository that continues the development of the original project.
Original project repository: https://github.com/AmazMod/AmazMod

AmazMod used to be a modified ("modded") Amazfit app, the companion app for Pace, Stratos, Stratos 3 and Verge watches built by Huami, changing and adding some of its features. But it has evolved to its own app that uses the data communication between Amazfit app on phone and the watch to implement its own notifications system and more.

### Some features:  
* Brand new notifications filter system, with the option to use customized ("canned") replies, actions, show images and more;  
* Battery/Heart-rate log and other info that can be viewed on phone app as a graph;  
* Control watch's screen brightness from phone ("Auto brightness" on watch must be *off* for this to work);  
* Option not to send notifications to watch when the smartphone's screen is on;
* An overlay button for a direct custom notification drawer;
* Receive messenger call notifications and maps navigation info on watch;
* File transfer to and from the watch over the air directly from smartphone;
* Built-in File Explorer on the watch with image and text viewing abilities;
* Shell execution optionality to the watch directly from the phone;
* Improved watch-smartphone connection tunnel (lower battery usage);
* Ability to display Emoji in messages;
* Various functions in watch widget menu (Wifi, Flash Light, QRCode, LPM, Admin, Reboot, Clean Memory);
* Battery notifications on watch/phone for watch/phone (low battery, fully charged);
* Re-order watch's widgets directly from phone/watch without limitation in enabled widgets number;
* Capture a watch screenshot;
* Push phone's battery, alarms and calendar events data available to Watchfaces/Widgets to use them (eg. [GreatFit](https://github.com/GreatApo/GreatFit), [Calendar Widget](https://github.com/GreatApo/AmazfitPaceCalendarWidget) & [Timeline Widget](https://github.com/GreatApo/Amazfit-Timeline-Widget));
* Uninstall applications from watch;
* Hourly chime feature (vibration every hour);
* More to come.

### Installation

###### Download links:
* Latest AmazMod (for phone): https://github.com/RedBloodDeadman/AmazModNext/releases/latest
* Latest service-release.apk (for watch): https://github.com/RedBloodDeadman/AmazModNext/releases/latest

###### Installation steps to use all features
1. Download and install latest AmazMod on phone;
2. Download and install latest service-release.apk on watch;
3. Restart both phone *and* watch;
4. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
5. Check if you see AmazMod widget on watch, if you don't then something went wrong, check Troubleshooting below;
6. Open AmazMod app on phone and configure it to your needs.
7. For better power managment, enable Device Admin in the widget menu or in the tweaking section on the phone app, or still use adb and run:`adb shell dpm set-active-admin com.amazmod.service/.receiver.AdminReceiver`

PS: On Verge, you must open AmazMod from the apps list the first time you install it to start the service, then go to the phone App to check connection (make sure you have set AmazMod as keep widget in settings to make it permanent). You may need to restart launcher or watch to see the widget properly.

###### Installation steps for minimal set of features (notifications filter only)
1. Download and install latest AmazMod from Play Store on phone;
2. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
3. Open AmazMod and make sure "Custom replies" are *disabled* and "Disable battery chart" is *Enabled* (i.e. battery chart is hidden), then restart phone;
4. Configure the other options to your needs (in this case, ignore the "Connecting" message shown in AmazMod). 

*PS: You may or may not disable Amazfit access to notifications, as long as it doesn't push notifications to watch. If you keep the access, then you can still use your watch to control music and most notifications dismissed on watch will also be removed from phone.*

### Troubleshooting

* How do I install the app on watch?  

You can use adb (all platforms), APKInstaller, Amazfit Tool or the provided installer if you are on Windows. To use adb, you need the binaries on your computer (download them from [Android SDK Platform tools page](https://developer.android.com/studio/releases/platform-tools), you may have them already if your computer runs Linux), then open Terminal/Command Prompt, change to the same folder as service-release.apk and run: 
1. `adb install -r service-release.apk`
2. `adb -d shell monkey -p com.amazmod.service 1 > NUL`
3. Check your smartwatch screen, the widget should be open
4. Go to the app in the phone and make it visible

Alternatively, you can use the EXE installers provided with each release, that will allow you to use a wizzard for a more convenient installation.

* I do not get notifications  

First, make sure you do not have Silent (Do not Disturb) mode enabled in both watch and phone. On watch it displays a "moon" icon when watch is unlocked but some custom watchfaces hide the status line so you must chech with the quick settings menu (swipe down form top), then tap the moon icon if it's blue to disable it. If it does not fix the problem, then make sure that AmazMod on phone has granted permissions to access notifications, restart both phone and watch and test notifications again ("standard" and "custom" notification tests from AmazMod app only works if you have service.apk installed on watch). Also keep in mind that you *must* keep stock Amazfit app installed and running on your phone for the current version of AmazMod to work!

For more info, please check the [FAQ](https://github.com/RedBloodDeadman/AmazModNext/blob/master/FAQ.md).

### Screenshots

<table>
	<tr>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_1.png"/>		
		</td>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_2.png"/>		
		</td>
				<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_3.png"/>		
		</td>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_4.png"/>		
		</td>
	</tr>
	<tr>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_5.png"/>		
		</td>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_6.png"/>		
		</td>
				<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_7.png"/>		
		</td>
		<td>
			<img src="https://github.com/RedBloodDeadman/AmazModNext/raw/dev/images/screen_8.jpg"/>		
		</td>
	</tr>
</table>

### License

This code is distributed using Creative Commons Attribution-ShareAlike 4.0 International license, meaning it's free to use and modify as long as the authors are given the appropriate credits, and the product is distributed using the same license or compatible one (less restrictive).


<a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-sa/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution-ShareAlike 4.0 International License</a>.
