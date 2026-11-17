# BUYLORA AI Studio V2

A zero-paid-API Android creative workspace.

## Included

- Ten creative workflows: image prompt, product photoshoot, brandkit, marketing, faceless-video storyboard, YouTube thumbnail, marketplace cards, character consistency plan, website draft, and 3D/audio production plan.
- Offline text templates and native PNG poster/card rendering.
- Save/share/copy output, local project history, and device text-to-speech.
- OpenRouter text assistant configured to request only `openrouter/free`.
- API key encrypted locally with Android Keystore AES-GCM.
- No API key in source code, GitHub, or network request logs.

## Free-only guarantee in the client

The app sends chat requests with `model: "openrouter/free"` and does not provide a paid-model fallback list. Do not add paid model IDs to fallback arrays. OpenRouter documents that this router selects from available free models only. Free model capacity and rate limits can change; current published free-tier limits are 20 requests/minute and 50 requests/day for free variants, subject to account eligibility and provider availability.

The app never asks for a payment method. If the free route is unavailable, it displays an error instead of intentionally switching to a paid model. The user is responsible for creating and protecting their own OpenRouter key; never paste it into chat or commit it to GitHub.

## Honest media limitations

The app creates image prompts and designed PNG layouts; it does not yet synthesize photorealistic humans or new product photographs. That requires a compatible image-generation model and runtime, which are not included. Video creates storyboards, not MP4. 3D/audio creates production briefs; narration preview uses Android TTS.

## Build

JDK 17, Android SDK 35, AGP 8.7.3, Gradle 8.9.
