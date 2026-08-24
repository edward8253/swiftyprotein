# SwiftyProtein Implementation Plan

Build a multi-screen Android application with a Splash screen, a mandatory Login screen (Firebase & Biometric), a Ligand search screen, and a Download confirmation screen.

## User Review Required

> [!IMPORTANT]
> **Firebase Setup**: I will add the Firebase dependencies, but for Firebase to work, you will need to:
> 1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
> 2. Add an Android App with the package name `com.swiftyprotein`.
> 3. Download the `google-services.json` file and place it in the `app/` directory of your project.
> 4. Enable **Email/Password** authentication in the Firebase Auth settings.

> [!WARNING]
> **Biometric Permissions**: The app will require `USE_BIOMETRIC` permission in the Manifest.

## Proposed Changes

### Dependencies & Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Personal/Projects/SwiftyProtein/gradle/libs.versions.toml)
- Add versions and libraries for:
    - Navigation Compose
    - Biometric KTX
    - Lifecycle Process (for foreground detection)
    - Firebase BOM & Auth
    - Coil (for image loading)

#### [MODIFY] [build.gradle.kts (app)](file:///C:/Personal/Projects/SwiftyProtein/app/build.gradle.kts)
- Apply Firebase plugins.
- Add the new implementation dependencies.

---

### Resources

#### [NEW] [ligands.txt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/res/raw/ligands.txt)
- Create a sample list of ligands (e.g., HEM, ATP, GLA).

---

### UI & Logic

#### [MODIFY] [MainActivity.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/MainActivity.kt)
- Set up the main `NavHost`.
- Implement app lifecycle listening to force navigation to the Login screen whenever the app returns to the foreground.

#### [NEW] [SplashScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/SplashScreen.kt)
- Display the molecule image.
- Timer to navigate to Login after 3 seconds.

#### [NEW] [LoginScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/LoginScreen.kt)
- UI for Email/Password login and account creation.
- Biometric authentication trigger.
- Integration with Firebase Auth.

#### [NEW] [LigandListScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/LigandListScreen.kt)
- Search bar.
- List of ligands loaded from `raw/ligands.txt`.
- Download logic with a progress bar using `HttpURLConnection` or `OkHttp`.

#### [NEW] [ConfirmationScreen.kt](file:///C:/Personal/Projects/SwiftyProtein/app/src/main/java/com/swiftyprotein/ui/screens/ConfirmationScreen.kt)
- Simple view showing "Download Successful".
- Back arrow navigation support.

---

## Verification Plan

### Automated Tests
- Build and run the app to ensure all screens are reachable.
- Verify that `ligands.txt` is correctly read.

### Manual Verification
1. **Splash**: Open app, wait 3 seconds, should see Login.
2. **Login**:
    - Try Email/Password (will need `google-services.json`).
    - Try Biometric.
3. **App Lifecycle**: Open the app, log in, go to Home screen, return to app. It should show the Login screen again.
4. **Search/Download**:
    - Search for a ligand.
    - Click to download.
    - Verify the progress bar appears.
    - Verify the confirmation screen shows up.
5. **Back Navigation**: Ensure back buttons return to previous states as expected.
