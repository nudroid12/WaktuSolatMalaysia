# Waktu Solat & Kiblat

Developer: NudroidLabs

Application ID: `app.nudroidlabs.waktusolat`

Current milestone: M8.4

Version: `0.8.4`

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

## M8.2 Final polish

- Home dipadatkan untuk memaparkan kandungan utama dalam satu skrin telefon biasa.
- Label Duha ditukar kepada Dhuha.
- Tetapan kini menggunakan seksyen yang boleh dikembangkan dan dicollapse.
- Jika Location dimatikan, app cuba dialog sistem Android untuk menghidupkannya tanpa meninggalkan app.
- Peranti tanpa resolution dialog masih mempunyai fallback ke tetapan Location.

## M8.3 fixes

- Restores the spacious scrollable Home layout from the earlier M8.2 design.
- Keeps Dhuha spelling, 12/24-hour format, GPS shortcut and all M8.2 Final settings.
- Qibla alignment now uses Android Vibrator and VibrationEffect instead of Compose haptic feedback.
- Qibla sound and the anti-jitter alignment latch remain enabled.

## M8.4 Azan

- Adds an offline built-in Fajr azan for Subuh.
- Adds an offline built-in normal azan for Zohor, Asar, Maghrib and Isyak.
- Keeps custom user-selected azan audio.
- Azan source can be switched between built-in audio and a custom file.
- Built-in audio is fetched by the build workflow from an Internet Archive item marked Public Domain Mark 1.0.
- Playback continues to use an alarm-class audio foreground service and exact alarm access.

## M8.4 v3

- Adds an azan-only volume slider from 0 to 100 percent without changing system volume.
- Moves the compact Location/Zone card below today's prayer times.
- Keeps manual zone selection and one-tap GPS in the compact location card.
- Compacts the data status card and replaces the large refresh button with a refresh icon.

## M8.4 v4 playback fixes

- Moves the compact location card above today's prayer times.
- Reduces the size of the next-prayer card without removing information.
- Azan volume changes now apply immediately to audio that is already playing.
- Tapping an azan test button again while a test is playing stops the test instead of restarting it.
- Test azan stops when the app leaves the foreground.
- Scheduled real azan is not stopped by leaving the app, so prayer-time playback still works as intended.

## M9 Release Candidate

M9 freezes product features and prepares the app for release validation.

- Version 0.9.0, versionCode 15.
- Release builds enable R8 code shrinking and Android resource shrinking.
- Conservative keep rules protect the foreground azan service, alarm receivers,
  and WorkManager refresh worker.
- Release signing is optional and activated only when all four signing
  environment variables are present.
- GitHub Actions builds debug APK, release APK, and release AAB, audits
  permissions/background behavior, records checksums and sizes, and preserves
  detailed diagnostics on failure.

## M9.1 Navigation & Exit Polish

- The compact location card is now the first Home card, above the next-prayer card.
- Android Back from 7 Hari, Kiblat, or Tetapan returns to Utama.
- Android Back from Utama opens an exit confirmation dialog.
- Confirming exit stops active azan/test playback and removes the app task.
- Scheduled prayer alarms, notifications, and periodic refresh remain intact after exit.
- Version 0.9.1, versionCode 16.

## M9.2 Doa & Cleanup

- Replaces the 7 Hari bottom tab with a lightweight offline Doa Harian tab.
- Includes 13 text-only entries with Arabic, Rumi, concise Malay meaning, and a reference on every card.
- No doa images, audio, network calls, or background work are added.
- Removes Qibla alignment vibration while keeping the alignment sound and anti-jitter gate.
- Keeps android.permission.VIBRATE because the existing prayer notification alert style still supports vibration.
- Removes the explanatory paragraph from the exit confirmation dialog.
- Version 0.9.2, versionCode 17.

## M10 Final Release 1.0.0

M10 freezes the M9.2 product exactly as approved and prepares the first final release.

- Version 1.0.0, versionCode 18.
- No UI redesign and no new product feature.
- Keeps R8 minification and Android resource shrinking enabled for release.
- Adds APK/AAB content-size analysis so the largest packaged files are visible.
- Adds dependency, permission, battery/background, service/worker, alignment,
  and release-output audits.
- Produces final debug APK, release APK, release AAB, R8 mapping, SHA-256
  checksums, signing report, size reports, and release-readiness report.

## M10.1 Self Updater

- Keeps final public version at 1.0.0 / versionCode 18 before first signing.
- Adds manual update check under Tentang aplikasi.
- Adds lightweight auto-check at most once every 24 hours when the app is opened.
- Uses a fixed GitHub-hosted update.json metadata URL.
- Downloads only HTTPS GitHub Release APK URLs.
- Verifies SHA-256, package name, versionCode, and signer certificate before install.
- Uses Android FileProvider and the normal Android package installer UI.
- No silent install and no always-running update background service.
