# ModDetech - App Installation Detection

A React Native application that monitors and notifies users about newly installed applications on Android devices.

## Features

- Real-time detection of newly installed applications
- Push notifications for app installations
- List view of detected applications
- Permission management for app detection
- Background service for continuous monitoring

## Prerequisites

Before you begin, ensure you have the following installed:
- [Node.js](https://nodejs.org/) (v14 or newer)
- [Java Development Kit (JDK)](https://adoptium.net/) (v11 or newer)
- [Android Studio](https://developer.android.com/studio)
- [Android SDK](https://developer.android.com/studio#command-tools)
- [React Native CLI](https://reactnative.dev/docs/environment-setup)

## Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/moddetech.git
cd moddetech
```

2. Install dependencies:
```bash
npm install
```

3. Install Android dependencies:
```bash
cd android
./gradlew clean
cd ..
```

4. Create a local.properties file:
```bash
cd android
echo sdk.dir=C:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk > local.properties
cd ..
```
Replace `YourUsername` with your Windows username.

## Building the APK

1. Generate a signing key (if you don't have one):
```bash
cd android/app
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
cd ../..
```

2. Build the release APK:
```bash
cd android
./gradlew assembleRelease
```

The APK will be generated at: `android/app/build/outputs/apk/release/app-release.apk`

## Installation on Device

1. Enable "Install from Unknown Sources" in your Android device settings
2. Transfer the APK to your device
3. Install the APK
4. Grant the following permissions when prompted:
   - Usage Access
   - Notifications
   - Package Query

## Development

1. Start the Metro bundler:
```bash
npm start
```

2. Run the app in debug mode:
```bash
npm run android
```

## Troubleshooting

If you encounter build issues:

1. Clean the project:
```bash
cd android
./gradlew clean
cd ..
```

2. Clear Metro bundler cache:
```bash
npm start -- --reset-cache
```

3. Rebuild node modules:
```bash
rm -rf node_modules
npm install
```

## License

This project is licensed under the MIT License - see the LICENSE file for details. 
