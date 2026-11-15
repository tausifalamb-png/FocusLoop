# BUYLORA AI Studio (Android)

An independent Android creative-workflow hub inspired by the public Higgsfield AI Skills repository. It is not an official Higgsfield app and does not bundle proprietary Higgsfield models or backend services.

## Implemented
- Native Kotlin Android app (min SDK 26, target SDK 35).
- Ten creative modules covering image generation, product photoshoot, brandkit, marketing, faceless video, YouTube thumbnails, marketplace cards, Soul ID workflow, website studio, and 3D/audio briefs.
- Live text-to-image requests through Hugging Face Inference Providers using `stabilityai/stable-diffusion-3-medium-diffusers`.
- Hugging Face token encrypted with Android Keystore AES-GCM.
- Generated PNG saved to `Pictures/BUYLORA AI Studio`.
- Other modules export creative briefs; they are not live media generators yet.

## Setup
1. Create a Hugging Face token with Inference permissions: https://huggingface.co/settings/tokens
2. Install the APK, enter the token in Provider setup, and save it. It is encrypted on-device.
3. Select AI Image Generator, enter a prompt, and tap Generate image.
4. Images are saved in `Pictures/BUYLORA AI Studio`.

Free-tier inference credits are small, subject to change, and not unlimited. See https://huggingface.co/docs/inference-providers/pricing. Higgsfield account access does not imply free Higgsfield API credits. Direct Higgsfield sign-in/job submission and live video/audio/3D generation are not implemented.

## Build
Requires JDK 17, Android SDK 35, Android Gradle Plugin 8.7.3, Gradle 8.9. From this folder run `gradle assembleDebug`. The workflow in the parent repository builds this project and uploads the debug APK artifact.

## Security
No token is hardcoded. Tokens are encrypted using Android Keystore AES/GCM. Cleartext traffic and Android backup are disabled.
