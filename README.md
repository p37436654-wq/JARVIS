# JARVIS Assistant

Native Android foundation for a voice-first personal assistant.

## Included
- Kotlin Android app
- Microphone foreground-service foundation
- AccessibilityService foundation for user-authorized phone automation
- Dark futuristic JARVIS UI
- Android runtime microphone permission
- Persistent service architecture

## Important
No private API key is embedded. Put your own AI provider credentials in a secure backend or protected runtime configuration before production use.

The current project intentionally does not claim unrestricted device control. Android security rules limit background microphone use, protected system settings, and certain UI/system actions.

## Next production modules
1. On-device wake-word engine (Porcupine or equivalent)
2. Speech-to-text pipeline
3. Secure AI gateway / command planner
4. Structured action allowlist
5. Accessibility gesture/text controller
6. Male TTS voice selection
7. Permission/onboarding flow
8. Boot/service recovery where Android permits
9. Command confirmation for sensitive actions
