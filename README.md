# Glass Dialer 0.1 Beta
Experimental LSPosed module for Samsung Phone. Calculator-style glowing number keys and translucent draggable navigation capsule for Keypad, Recents and Contacts. Based on class names from the supplied Samsung Dialer APK. This is a custom glass approximation, not true optical refraction or a copy of proprietary calculator rendering.

## Install
Install the APK from Actions, enable Glass Dialer in LSPosed and select Samsung Phone (com.samsung.android.dialer). Force stop and reopen Phone. Disable the module and reopen Phone if anything is wrong. Do not rely on this beta for emergency calls; verify normal dialing, long press, and tab switching before routine use. Device compatibility and refresh rate have not been verified. No SystemUI hook or root commands are needed.

## Build
GitHub Actions runs assembleDebug using Java 17, Gradle 8.11.1 and Android SDK 35. APK signing is verified by apksigner. Debug signatures can differ across runs. Only compile-time Xposed API stubs are included in the project, not bundled in the APK.
