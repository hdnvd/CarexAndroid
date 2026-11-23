# Carsa Android (WebView client)

Thin Android wrapper that shows the Carsa PWA (`https://app.mycarsa.ir`) in a
WebView. Modeled on the PulNegar Android client.

- Package: `ir.faravandgroup.carsa`, label `کارسا`, minSdk 24, targetSdk 34.
- Each page load on `*.mycarsa.ir` gets `client=app&version=…&mkt=…&age=…`
  appended (same query string as PulNegar, `hasSms=false`), and the user agent
  ends with `sweetwebappclient <versionCode>`.
- `/sweetsoft/command/exitapp` and `/sweetsoft/command/clearwebcache` work as in
  PulNegar. Other `sweetsoft/command/*` URLs are ignored.
- `tel:`, `mailto:`, `intent:` and other non-http links, plus map.ir, Google
  Maps, Neshan, WhatsApp, Telegram, Bale, Eitaa, Instagram, Bazaar and Myket,
  open in their own apps.
- Geolocation (`navigator.geolocation`) asks for the Android location
  permission. Only `*.mycarsa.ir` pages can get the location.
- Deep links: `https://mycarsa.ir/openapp/<path>?<query>` and the same on
  `app.mycarsa.ir` open `<path>?<query>` inside the app.
- Deadline notifications: pages on `https://app.mycarsa.ir`
  call `window.CarsaNotifications.syncDeadlines(json)` with the driver's active
  deadlines; `business/deadlines` schedules local notifications at 10:00 for 30,
  7 and 1 day(s) before and on the due date, re-schedules after reboot, app
  update and time changes, and asks for the Android 13+ notification permission
  once. Tapping a notification opens `/carsa/deadlines`.
- Store channel: change `MarketName` in `WebAppMainActivity` (Myket by
  default) before building an APK for each market.

## Signing

The configured release keystore and `keystore.properties` hold the release key and
its password. Both are gitignored. **Back them up somewhere safe**: losing the
keystore means you cannot publish updates to the same listing.

If `keystore.properties` is missing, the release build comes out unsigned.

## Build

```
gradlew.bat assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk` (signed).
Increase `versionCode` in `app/build.gradle.kts` for every published update.

`gradle.properties` has `org.gradle.offline=true`, as in PulNegar, so
dependencies come from the local Gradle cache. Remove that line if you add a
new dependency.
