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

## v0.7 — source audit
Keep native digit/letter stack intact, shared radius across keys, avoid repeated spring restart during layout, bound gradient stops, handle interrupted gestures, restrict red feedback to delete background instead of entire call layout, avoid duplicate native tab selection. Added pure-Java geometry regression checks to CI; these are not Android UI tests. Release publish retries and Actions artifact fallback. Device testing remains necessary.

## v0.8
Recording-based changes: keep touch receiver fixed while animating key drawable recoil; square bitmap alpha-mask pass with ordered gradient stops; center native vertical number/letter stacks while leaving horizontal layout unchanged; observe native backspace touch without intercepting its action; animate newly appended digit from right in verified EllipsisEditText using temporary drawing span, without altering dialed text. Text-entry effect recreates the calculator's animateInFromRight behavior, not its proprietary renderer. Test single taps, long holds, fast entry, cursor insertion and backspace. Runtime and all orientations remain unverified.

## v0.9 stability audit
Latest-edit generation guard prevents stale number-entry animation; release animation resources on detach; fit the full recoil halo inside key bounds; center complete visible native label groups without changing text or order; discover sibling backspace icon; fit the entire slider capsule inside track; use actual selected-tab width on drag and consume cancelled pointer streams. No per-frame native scale writes. Added regression tests for these pure geometry and text eligibility helpers. Android UI, real glass fidelity, actual delete behavior and 120 Hz frame pacing still require testing on the phone.

## v1.0 beta
Avoid span changes during beforeTextChanged; clear on latest edit; respect native animation disable setting; preserve disabled/stateful digit colors; track delete pointer and slop; consume cancelled tab gesture through its end; cap glow bitmap allocations; constrain narrow slider insets. Pure Java regression checks are not a substitute for Android runtime tests. Device behavior, native hooks on firmware variants, and 120 Hz remain unverified.

## v1.1 — visible motion
Based on the recorded comparison showing lit keys but missing number-line motion: replace span-based entry animation with draw-only native number-line slide/fade, triggered by an added dial character even when formatting adds spaces. Does not move a single digit independently. Apply synchronized visual recoil to key labels and background with 13% compression, retain short-tap rebound and preserve touch geometry. Respect Android animation settings. Runtime effect and actual frame rate still unverified.

## v1.2
Clean number-only keypad: hide secondary text and known auxiliary icons visually; preserve native long-press actions and accessibility semantics. Center the primary digit using its layout/ink metrics. Per-glyph entry animation uses a clipped native Layout redraw, independent of cursor selection and formatting separators; existing digits retain their layout. Restore native backspace idle tint and serialize touch/press feedback. No real-device verification; custom animation is a recreation, not a proprietary calculator port. Test RTL, ellipsis, large text and backspace on device.

## v1.3
Remove custom slider and all custom tab gesture hooks. Native Samsung tabs restored. Backspace uses native shape and stateful red icon tint plus red native ripple when present; no custom recoil, circle or glow animator. Individual digit entry uses direct glyph drawing at native layout position with smoothstep timing, no per-glyph saveLayer or Layout.draw. Native formatting, selection and number text remain unmodified. Actual smoothness and stock delete behavior require device verification.

## v1.4
Increase circle maximum diameter from 72dp to 78dp with adaptive bounds and glow margins. Keep native Samsung tab visual and click behavior; scoped horizontal drag translates the existing selected indicator if present, restores it and original clipping flags on release/cancel, then selects the destination via native click. If the indicator cannot be resolved, swipe-to-select on release still works. No custom glass drawable. Deduplicate formatted digit callbacks, cancel old animators on reduced-motion transition, cache glow shaders, avoid redundant key rebinding. Tests cover geometry and gesture thresholds, not device UI. Runtime appearance, native tab selection and 120 Hz require testing.

## v1.5
Remove the generic dialpad_key_image from key 1 (APK verified resource ID names: one, dialpad_key_image, dialpad_key_1_letter_text_view). Suppress only visual hint drawing and preserve native voicemail long press. Reapply after view inflation; protect indicator cleanup on interrupted swipes and reject strongly vertical releases. Refresh digit state on detach. Larger key sizing, native tab appearance, red-only delete, and AMOLED remain. Runtime verification still required.

## v1.6
Screenshot measurements (754px width): Calculator digit ink heights 44–46px versus dialer 36–38px. Calculator APK number style: sec family, weight 300, 32dp base; CustomButton applies bold style when Settings.Global bold_text is enabled. Use the same family/weight and device bold preference, draw the digits centered by ink in the same canvas as key circle, target 0.335 diameter cap height. Native labels retained for semantics and restored on detach. Prewarm glow cache; one clock for synchronized glow/recoil with continuous quick-tap rebound and 3dp recoil. Entry timing 250ms and curve (.22,.25,0,1) based on Calculator animateInFromRight; effect is a simplified recreation, not direct reuse of its multi-animator renderer. Runtime font resolution, glyph fidelity and frame pacing remain unverified.
