# Waktu Solat & Kiblat

Developer: NudroidLabs

Application ID: `app.nudroidlabs.waktusolat`

Current milestone: M8.2

Version: `0.8.2`

## Current scope

M8 carries the complete M5, M6 and M7 functionality into the release-hardening milestone.

- Official JAKIM e-Solat weekly prayer data with validated local cache.
- Manual JAKIM zone selection and conservative one-shot location-assisted zone suggestion.
- Today, seven-day schedule, qibla and settings screens.
- Per-prayer notifications and early reminders.
- Sound, vibration and silent notification styles.
- Optional user-selected azan audio with per-prayer controls.
- Rescheduling after reboot, date, time and timezone changes.
- Qibla sensors run only while the qibla screen is visible.
- WorkManager refresh remains once every 24 hours and requires connectivity.

## M8 reliability and hardening

- Fixes the M7 validator regression test so it contains a genuine out-of-order prayer sequence.
- Adds validator regression coverage for invalid time values, duplicate dates, bad status, empty schedules and cache-date boundaries.
- App version shown in Settings now comes from `BuildConfig.VERSION_NAME`, removing stale hard-coded version text.
- JAKIM User-Agent version also comes from `BuildConfig.VERSION_NAME`.
- GitHub Actions separates unit tests, debug/release compilation and lint so diagnostics identify the exact failed stage.
- JUnit XML and HTML test reports are included in failure diagnostics.
- Both debug and unsigned release variants must compile.
- Both debug and release lint must pass with warnings treated as errors.
- Static permission and battery audits reject background-location permission, continuous location updates, cleartext traffic, analytics/ad dependencies and accidental always-on qibla sensor behaviour.
- APK size is reported for visibility only. It is not a release gate.

## Prayer time source

The app reads prayer schedules from the official JAKIM e-Solat domain:

`https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=WLY01`

The zone code is replaced with the selected JAKIM zone. Network and cached responses are validated before use.

## Battery approach

- No background location permission.
- Location detection is one shot.
- Qibla sensors are active only on the qibla screen.
- Prayer schedules are cached locally.
- Periodic refresh remains once every 24 hours and requires network connectivity.
- AlarmManager schedules prayer events instead of running a permanent service.
- The media foreground service exists only while azan audio is actually playing.

## Build

The project targets Android API 37 and uses JDK 17, Gradle 9.4.1 and Android Build Tools 36.0.0 in GitHub Actions.

M8 is committed only after unit tests, debug and release compilation, debug and release lint, and the hardening audit all pass.

## M8.2 polish

- Home GPS detects whether phone Location is disabled before requesting a fix.
- If Location is disabled, an in-app prompt offers to open Android Location settings.
- Location detection resumes after returning when Location has been enabled.
- Qibla alignment gives one short haptic confirmation and acknowledgement tone.
- Qibla feedback is latched and only rearms after moving clearly away from alignment.
