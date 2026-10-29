# SMS Forwarder (Android)

Forwards incoming SMS from the device to an HTTP endpoint you control, so a server can react to messages without polling a phone.

Kotlin, no analytics, no third-party SDKs.

## How it works

1. `SmsReceiver` listens for `SMS_RECEIVED` and hands the message to a work request.
2. `SmsWorker` (WorkManager) POSTs the message to the webhook stored in the app's preferences.
3. `KeepAliveService` is a foreground service that keeps the receiver alive on devices with aggressive battery managers.
4. `NotificationReader`, optional, picks up messages that arrive as notifications rather than SMS, which happens with some banking and dual-SIM setups.

Payload sent per message:

```json
{"sender": "+92...", "message": "text"}
```

## Build

```bash
./gradlew assembleDebug
```

`.github/workflows/build.yml` builds a debug APK on push, which is how the test builds are produced.

## Setup

1. Install the APK.
2. Grant the SMS permission and turn off battery optimisation for the app.
3. Open the app, set the webhook URL, save.

## Limitations

- A failed POST is retried on WorkManager's default policy only; there is no custom backoff yet.
- No payload signing or HMAC header. Put the endpoint behind TLS and verify the sender on the server.
- Dual-SIM sender detection is best effort.

## Privacy

The app reads every SMS on the device and sends it to the URL you configure. Run it only on a device you own and point it only at a server you control. Forwarding someone else's messages without their consent is illegal in most jurisdictions.

## Licence

MIT, see `LICENSE`.
