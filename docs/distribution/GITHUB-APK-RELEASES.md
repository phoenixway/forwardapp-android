# ForwardApp APK releases for Obtainium

The [ForwardApp APK repository](https://github.com/phoenixway/forwardapp-apks) is
the public download channel for signed experimental Android builds. Add that
repository URL in Obtainium after the first release is published. Each release
contains a single standalone universal APK.

## Enable publishing

In the Android source repository's Actions settings:

1. Add the `ACTIONSSECRET` secret. Use a fine-grained GitHub token scoped to
   `phoenixway/forwardapp-apks` with the repository permission **Contents: Read
   and write**. Renew the secret when the token expires.
2. Set the `APK_RELEASE_REPOSITORY` Actions variable to
   `phoenixway/forwardapp-apks` to enable publishing.

Run **Build Android APK** on `dev` with flavor `exp` and build type `release`.
Each successful build of this variant is eligible for publication. Actions
also keeps its usual downloadable artifact.

## Versioning and updates

- Publishing uses the Android build workflow run number as the APK
  `versionCode`. It must exceed the base Gradle version code. The release tag
  matches the APK's `versionName`, for example `1.20.0.590-exp`.
- The package ID and release signing configuration match the existing signed
  `exp:release` build. A device already using that build can update in place
  when the signing certificate matches and the release has a higher version code.
- Publication verifies the APK signature and uploads the APK to a draft release
  before making it public. A retry can finish an interrupted draft. Older runs
  that finish late do not replace or supersede a newer published build.
- Once you install from this channel, use it for subsequent updates. Standard
  builds use the base version code and Android treats a lower version code as a
  downgrade.

[Obtainium GitHub source documentation](https://wiki.obtainium.imranr.dev/sources/#github)
