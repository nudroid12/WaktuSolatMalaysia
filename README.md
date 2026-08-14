# Waktu Solat Malaysia

Developer: NudroidLabs

Application ID: `app.nudroidlabs.waktusolat`

Current milestone: M5

Version: `0.5.0`

## M5 scope

M5 is the product polish milestone. It keeps the M4 prayer, notification, azan and qibla foundation while improving the daily user experience.

- Home prioritises the next prayer, countdown, current JAKIM zone and today's prayer times.
- The 7 day view highlights the current day and keeps the schedule compact.
- Settings are grouped into appearance, location and zone, reminders, azan and app information.
- Appearance supports system, light and dark modes.
- Location remains one shot only and is never tracked continuously in the background.
- Qibla sensors remain active only while the Qibla page is visible.
- JAKIM data reports whether it came from the network, fresh cache or fallback cache.
- Notifications, early reminders, exact alarms and custom azan audio remain available.
- No analytics, advertising or tracking SDK is included.

## Prayer time source

The app reads prayer schedules from the official JAKIM e-Solat domain:

`https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=WLY01`

The zone code is replaced with the selected JAKIM zone. The app validates the response and keeps the latest valid schedule in local cache for resilience when the network is unavailable.

## Battery approach

- No continuous background GPS.
- No continuous qibla sensor outside the Qibla screen.
- Prayer schedules are cached locally.
- Background refresh uses WorkManager on a low frequency schedule.
- Prayer alarms are scheduled rather than implemented with an always running service.
- Azan playback starts only when a scheduled prayer alarm requests it.

## Build

The project targets Android API 37 and uses JDK 17, Gradle 9.4.1 and Android Build Tools 36.0.0 in GitHub Actions.

M5 is committed only after unit tests, debug APK build and Android lint complete successfully.
