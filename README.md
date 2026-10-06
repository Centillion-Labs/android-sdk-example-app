# Centillion Android example app

A Jetpack Compose chat app using the [Centillion Android SDK](https://github.com/Centillion-Labs/centillion-android-sdk). It demonstrates streaming replies, Markdown, conversation history, pagination, retry, and native tool execution.

## Run

Requires JDK 17+, Android SDK 35, Android Studio compatible with AGP 8.13.2, and an Android 7.0+ device or emulator. The project includes Gradle 9.2.1 and uses Kotlin 2.2.20.

```sh
git clone --recurse-submodules https://github.com/Centillion-Labs/android-sdk-example-app.git
cd android-sdk-example-app
```

Open this folder in Android Studio, let Gradle sync, select the `app` target, and run. The public SDK is pinned to version `0.1.0` as a Git submodule under `vendor/centillion-android-sdk`. If you cloned without submodules, run `git submodule update --init --recursive` before syncing.

In Centillion, open your agent's Channels → Android SDK → Overview, enable Visibility, and save. Copy the agent ID from Setup and enter it in the app. The SDK base URL defaults to `https://app.cntillion.com/api/sdk`. Change it if you use a different Centillion host. No API key is needed. Keep identity signing secrets on your backend.

The app registers `get_app_version`. To try it, create a Client action with that exact name, select Wait for response, and enable the action for Android SDK. Ask the agent to check the app version.

## Verify

```sh
./gradlew :app:assembleDebug :centillion-sdk:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

For local development, the Android emulator uses `http://10.0.2.2:3000/api/sdk`. Alternatively, run `adb reverse tcp:3000 tcp:3000` and use localhost. Local HTTP is allowed only in Debug. Use HTTPS on physical devices and in production.

For a live smoke run:

```sh
adb shell am start -n com.centillion.sample/.MainActivity \
  --es agentId YOUR_AGENT_ID --es baseUrl http://10.0.2.2:3000/api/sdk --ez smoke true
adb logcat -d -s CENTILLION_SMOKE
```

Enable the `get_app_version` action first. The smoke run checks a reply, pagination, conversation listing, retry, and native tools. Use a dedicated test agent because this creates conversations and consumes reply credits.

## Source

`app/src/main/java/com/centillion/sample/MainActivity.kt` contains the setup screen, chat, history, and smoke flow. Read the SDK's [API guide](https://github.com/Centillion-Labs/centillion-android-sdk#client-and-streaming) for signed user identity and custom interfaces.

## License

MIT. See [LICENSE](LICENSE).
