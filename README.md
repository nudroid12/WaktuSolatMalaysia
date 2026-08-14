# Waktu Solat Malaysia

Developer: **NudroidLabs**  
Application ID: `app.nudroidlabs.waktusolat`  
M1 version: `0.1.0`

## M1 scope

- Reads prayer schedules directly from the official JAKIM e-Solat domain.
- Shows Imsak, Subuh, Syuruk, Duha, Zohor, Asar, Maghrib and Isyak.
- Shows the next obligatory prayer and a live countdown.
- Includes the 60 zone codes currently listed on JAKIM e-Solat's zone table.
- Saves the selected zone locally.
- Caches the latest successful weekly response for resilience when the network is unavailable.
- Shows JAKIM server time and zone qibla bearing returned by the source.

## Data source

Official JAKIM e-Solat endpoint used by the app:

`https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=WLY01`

The zone code is replaced with the user's selected JAKIM zone.

Important: the endpoint is publicly reachable on JAKIM's official domain, but it is not presented on the e-Solat site as a formally documented public developer API. Therefore the app validates the returned status, zone and time fields, and caches the last valid response.

## Build on GitHub Actions

Upload the project to a GitHub repository and run **Actions > Android Build > Run workflow**. A successful build uploads `Waktu-Solat-Malaysia-debug` as an Actions artifact containing the debug APK.

The workflow uses JDK 17, Gradle 9.4.1, Android API 37 and Build Tools 36.0.0.

## Next milestone

M2 should add exact automatic zone detection, notification scheduling and azan behaviour. Automatic GPS-to-zone mapping is intentionally not guessed in M1 because JAKIM schedules are zone/district based and accuracy is more important than an approximate nearest-zone selection.
