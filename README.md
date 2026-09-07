# Swifty Protein
Android application to show in 3D with OpenGL various type of proteins from a list

## Guide to upload apk to external device

### Download SDK manager and tools on the computer
1. Make sure to have OpenJDK installed, if not, search, download and install it on your computer
2. Go to this [website](https://developer.android.com/studio#command-tools), go to the end of the page and download *commandlinetools-<computer-architecture>...*
3. Create *android* folder in *$HOME* directory and unzip the file downloaded into that folder
4. Execute these commands on a shell:
```bash
# Create folder to contain unzipped files
cd $HOME/android/cmdline-tools
mkdir tools
mv -i * tools

# Export necessary variables (I put all of these in $HOME/.bashrc for them to be executed automatically when a new shell is opened)
export ANDROID_HOME=$HOME/android
export PATH=$ANDROID_HOME/cmdline-tools/tools/bin:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools/:$PATH

# sdkmanager is for UNIX, sdkmanager.bat is for Windows 
# Test if everything went well
sdkmanager

# Install necessary packages
sdkmanager --install "platform-tools" "platforms;android-29" "build-tools;29.0.2" "emulator"
```

### Connect/setup mobile phone
1. Activate *Developer mode* on your Android phone : Tap *Build number* option 7 times
2. Enable *USB debugging* : Go to Settings > System > Developer option and toggle *USB debugging* ON
3. Connect the mobile phone to the computer via cable
4. Accept popup *Allow USB debugging?* on the device
5. Test if device is connected correctly and accessible by the computer via this command line:

```bash
$ adb devices
List of devices attached
1234567890ABCDEF    device
```

### Compile/transfer/execute the app on the external device

To run the app on the external device, first the app project must be compiled and must have produced a *.apk* file in its default folder: *<ProjectFolder>/app/build/outputs/apk/debug*
After this file is created, it can be uploaded onto the device with:
```bash
cd <ProjectFolder>
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The app can then be launched by the user on the device screen, or by executing this command on the terminal:
```bash
adb shell am start -n com.swiftyprotein/.MainActivity
```

