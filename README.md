# Waktu Solat Malaysia

Developer: NudroidLabs

Application ID: `app.nudroidlabs.waktusolat`

Current version: `0.4.0`

## Fungsi utama

* Jadual waktu solat terus daripada domain rasmi e-Solat JAKIM.
* Imsak, Subuh, Syuruk, Duha, Zohor, Asar, Maghrib dan Isyak.
* Solat seterusnya dengan countdown yang hanya berjalan ketika halaman Utama aktif.
* Senarai 60 zon JAKIM dan pilihan zon manual.
* Pengesanan lokasi sekali sahaja apabila pengguna meminta, tanpa GPS latar belakang.
* Jadual 7 hari.
* Notifikasi masuk waktu untuk Subuh, Zohor, Asar, Maghrib dan Isyak.
* Pilihan peringatan awal 5, 10 atau 15 minit.
* Sokongan alarm tepat apabila pengguna memberikan akses Android.
* Kompas kiblat berdasarkan koordinat terakhir, sensor rotation vector dan pembetulan deklinasi magnet.
* Audio azan penuh menggunakan fail audio yang dipilih sendiri oleh pengguna.
* Audio azan dimainkan melalui foreground media playback hanya ketika masuk waktu dan berhenti apabila audio tamat, dengan had keselamatan 10 minit.
* Cache jadual JAKIM untuk mengurangkan penggunaan rangkaian.
* WorkManager hanya menyemak jadual sekali sehari apabila notifikasi dihidupkan.
* Tiada analytics, iklan atau tracker.

## Sumber data

Aplikasi menggunakan endpoint pada domain rasmi e-Solat JAKIM:

`https://www.e-solat.gov.my/index.php?r=esolatApi/TakwimSolat&period=week&zone=WLY01`

Kod zon diganti mengikut zon pilihan pengguna. Endpoint ini boleh dicapai pada domain rasmi JAKIM, tetapi aplikasi tidak menganggapnya sebagai API pembangun yang dijamin stabil. Respons diperiksa sebelum digunakan dan respons terakhir yang sah dicache.

## Privasi lokasi

Aplikasi tidak meminta `ACCESS_BACKGROUND_LOCATION`. Lokasi hanya diambil selepas pengguna menekan butang kesan lokasi. Koordinat terakhir disimpan pada peranti untuk fungsi kiblat. Sensor kompas hanya didaftarkan ketika halaman Kiblat sedang dipaparkan dan dinyahdaftar apabila pengguna meninggalkan halaman tersebut.

## Audio azan

Tiada rakaman azan dibundel dalam APK. Pengguna memilih fail audio sendiri melalui Android document picker. Ini mengelakkan APK menjadi besar dan tidak memerlukan permission storan umum. Main balik penuh hanya dicuba apabila alarm tepat tersedia. Jika tidak, notifikasi waktu solat masih berfungsi mengikut keupayaan Android.

## Build

Konfigurasi semasa menggunakan Android API 37, JDK 17 dan Gradle 9.4.1 melalui GitHub Actions. Workflow M4 hanya commit kod `0.4.0` selepas unit test, APK debug dan Android Lint semuanya lulus tanpa error atau warning.

M4 memfokuskan kestabilan, pembersihan lint dan penggunaan bateri yang minimum.
