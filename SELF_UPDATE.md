# Self Update

The app checks this repository's `update.json` at most once every 24 hours when
the app is opened. Users can also check manually from Settings > Tentang aplikasi.

For a future release:

1. Build and sign the APK using the same final signing key as 1.0.0.
2. Upload the signed APK as a public GitHub Release asset.
3. Calculate SHA-256 for the exact signed APK.
4. Update `update.json` with the new versionCode, versionName, GitHub Release APK
   URL, SHA-256, and short release notes.
5. Commit `update.json` to the `main` branch.

Security checks performed before the Android installer is opened:

- remote versionCode must be newer
- APK URL must be HTTPS on github.com
- downloaded APK SHA-256 must match update.json
- APK package name must be app.nudroidlabs.waktusolat
- APK versionCode must match update.json
- APK signer certificate must match the currently installed app

Android still shows its normal package installer confirmation. No silent install
is attempted.

The app declares REQUEST_INSTALL_PACKAGES because it is currently distributed
outside Google Play. Re-evaluate this self-updater and permission before a future
Google Play release.

## Android signer compatibility

The updater signer check preserves minSdk 26 support with API-gated helper methods:
- API 26-27 uses GET_SIGNATURES.
- API 28-32 uses GET_SIGNING_CERTIFICATES.
- API 33+ uses PackageInfoFlags with GET_SIGNING_CERTIFICATES.

All branches compare SHA-256 digests of the signing certificates.

## Lint propagation

API 28 and API 33 helper methods use AndroidX `@RequiresApi`. Their callers
perform explicit `SDK_INT` checks before entering those helpers, allowing lint
to verify the API requirement is propagated safely instead of merely suppressed.
