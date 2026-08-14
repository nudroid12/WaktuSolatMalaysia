# Waktu Solat Malaysia

Developer: NudroidLabs
Application ID: `app.nudroidlabs.waktusolat`
Current milestone: M2
Version: `0.2.0`

## Data source

Prayer schedules are read from the official JAKIM e-Solat domain:

`https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=WLY01`

The zone code is replaced with the user's selected JAKIM zone.

The endpoint is publicly reachable on JAKIM's official domain, but it is not presented on the e-Solat site as a formally documented public developer API. The app validates the returned status, zone and time fields, then caches the last valid response.

## M2 scope

- Imsak, Subuh, Syuruk, Duha, Zohor, Asar, Maghrib and Isyak from e-Solat JAKIM.
- Live next-prayer countdown.
- 60 JAKIM zone codes.
- Manual zone selection remains available at all times.
- User-triggered location detection using Android location and reverse geocoding.
- Location detection produces a zone suggestion only when the administrative address maps uniquely to the official JAKIM zone names.
- Ambiguous results are not guessed and do not silently change the selected zone.
- Optional prayer notifications for Subuh, Zohor, Asar, Maghrib and Isyak.
- Per-prayer notification switches.
- Android 13+ notification runtime permission handling.
- Android 12+ exact-alarm access handling, with inexact fallback if exact access is unavailable.
- Future prayer alarms are rebuilt after time changes, timezone changes, reboot, or exact-alarm permission changes.
- WorkManager refreshes the JAKIM schedule periodically so future alarms remain populated.
- Weekly response cache remains available if the network is temporarily unavailable.

## Location accuracy note

JAKIM prayer schedules use administrative prayer zones, including several special or split zones. This project does not use a nearest-coordinate approximation. Android reverse geocoding is used to obtain administrative place names, then those names are matched against the official JAKIM zone list. If a unique match cannot be established, the user must select the zone manually.

## Notification accuracy note

Exact alarms require special access on recent Android versions. If the user has not granted exact-alarm access, Android may deliver reminders later than the precise prayer-time minute. The app exposes this state and provides a shortcut to the system setting.

## Build

The project targets Android API 36 with JDK 17 and Gradle 9.4.1. GitHub Actions runs unit tests before assembling the debug APK.
