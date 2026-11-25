# Glass Dialer 0.5 AMOLED
Experimental LSPosed module for Samsung Phone. Calculator-style glowing number keys and translucent draggable navigation capsule for Keypad, Recents and Contacts. Based on class names from the supplied Samsung Dialer APK. This is a custom glass approximation, not true optical refraction or a copy of proprietary calculator rendering.

## Install
Install the APK from Actions, enable Glass Dialer in LSPosed and select Samsung Phone (com.samsung.android.dialer). Force stop and reopen Phone. Disable the module and reopen Phone if anything is wrong. Do not rely on this beta for emergency calls; verify normal dialing, long press, and tab switching before routine use. Device compatibility and refresh rate have not been verified. No SystemUI hook or root commands are needed.

## Build
GitHub Actions runs assembleDebug using Java 17, Gradle 8.11.1 and Android SDK 35. APK signing is verified by apksigner. Debug signatures can differ across runs. Only compile-time Xposed API stubs are included in the project, not bundled in the APK.

## v0.2
Based on the user's recorded comparison: calculator-dark key surfaces, diffuse green/cyan halo without bright outline, fixed teal digit feedback, and a neutral silver glass tab material with smoother stretch and snap. Native tab actions and labels are preserved. No true background refraction. Runtime validation pending.

## v0.3 – calculator material
Uses original APK resource palette: calc_keypad_number_button_bg, keypad_number, keypad_number_gradient_normal, keypad_normal_gradient_start/end, keypad_clear and keypad_clear_gradient_start/end. Samsung SeslRecoilAnimator DEX specifies 100 ms press, 350 ms release and 3 dp scale amount. The glow and interpolators are recreated, not the original Samsung renderer. Dark mode targeted keypad backgrounds are #000000. Confirm black surface, delete hook and native dial behavior on device. Reduced slider contrast.

## v0.4
Correct normal-dialer backspace layout (DialpadCallButtonLayout with delete_button_background_layout and delete_button_image, from res/oC1.xml); stop competing stock keypad visual animator when present; move diffuse glow above the opaque key fill so it is not hidden, slightly larger keys, reduce metallic rim on tabs. This remains a custom material rather than a pixel-identical port. Validate on device.

## v0.5 — AMOLED-only test
Enable system Dark mode. Samsung Dialer resource allowlist and guarded neutral surface recoloring target main app windows, keypad, recents, contacts, search, settings, cards and dialogs. Full coverage is not device-verified. Text, colored controls, ripple/selector state and calling behavior are not intentionally changed. Previous custom keypad/slider hooks are kept in source but disabled in this test build. InCallUI and separate Contacts processes are not targeted. Light mode is stock. Debug signatures may differ between runs.

## v0.6
Retains v0.5 dark AMOLED theme; re-enables experimental keypad and slider. Same visual radius across visible keypad keys, centered primary digits with guarded adjustment, spring release, improved short-tap visibility, velocity-assisted drag settling and native tab model selection fallback on the supplied firmware. No claim of device validation, exact glass refraction or universal alignment. Test portrait, landscape, font sizes and all three tabs. Disable module if dialing is affected.
