# BUYLORA AI Studio — Zero-Budget Offline Edition

An independent Android creative-workflow app. This version is designed to work offline without paid API keys, subscriptions, cloud inference credits or account tokens.

## What works offline
- Ten creative modules: image prompt builder, product photoshoot, brandkit, marketing, faceless video, YouTube thumbnail, marketplace cards, consent-based character workflow, website studio, and 3D/audio briefs.
- Deterministic brief templates generated on-device.
- Copy and share generated briefs.
- Save briefs privately in app-local storage and reopen them.
- No network permission, API key, billing setup or account login required.

## Important capability boundary
This build creates prompts, plans and production briefs. It does **not** render image, video, audio or 3D files. Real image generation fully offline would require integrating and downloading a compatible on-device model/runtime; that model is not bundled here, and performance depends on the phone's RAM, storage and chipset. No unlimited-free cloud generation is promised.

## Build
Requires JDK 17, Android SDK 35, Android Gradle Plugin 8.7.3 and Gradle 8.9. From this folder run `gradle assembleDebug`. The parent repository's GitHub Actions workflow builds this project and uploads a debug APK artifact.

## Privacy
The creative brief library is stored in this app's private SharedPreferences. The app does not send prompts or saved briefs to a server.
