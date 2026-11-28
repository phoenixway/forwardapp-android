# ForwardApp Peer Updates

Open the global More menu and choose **Peer Updates**. The source chooses **Share my version** and keeps the screen visible. The receiver chooses **Find updates** or enters the displayed IPv4 address and port under **Connect manually**. To download, enter the share code shown on the source. After verification, choose **Install update** and approve the Android installer prompt.

## Packaging and trust

ForwardApp reads the installed APK through `ApplicationInfo.sourceDir`. The universal and ABI-specific Gradle APKs are standalone. App Bundle installations with populated `splitSourceDirs` show an unsupported-format message. **Export APK** uses Android's document picker (including USB when available) or a read-only FileProvider URI with the system Share chooser. On a phone with no ForwardApp, open the saved APK with Android's package installer. The optional browser link works while the source is actively sharing.

Each update must have a larger `longVersionCode`, the same application ID, the exact signer set, and the same authenticated flavor and build type. The packaged manifest carries `forwardapp.peer.variant` in the form `prod:release`, `exp:local`, etc. A higher prod build cannot replace exp, and a release build cannot replace local through this feature. APKs for a different ABI or a newer minimum Android version are incompatible. Older ForwardApp builds can bootstrap through exported APKs using Android's normal package installer rules. The installer stages a full replacement for the existing package, and Android controls approval, signature enforcement and installation. The app reads the actual installed version after relaunch. Database migrations use their existing entry points.

## Wire protocol v1

- NSD service type `_fa-update._tcp.`. The service announces the source's TCP port. Protocol v1 uses IPv4. Port 8765 is the default.
- `GET /v1/package` returns UTF-8 JSON with `protocol` (1), `applicationId`, `versionName`, `versionCode`, `variant`, `size` (bytes), `sha256`, `certificates` (sorted SHA-256 fingerprints of signing certificates), `minSdk`, `deviceName` and `abis` (sorted native ABI names). Hex strings use lowercase. The receiver caps metadata at 8 KiB and accepts APKs up to 1 GiB.
- `GET /v1/apk/{code}` streams the installed standalone APK. The code is 96 random bits expressed as 24 lowercase hex digits. The server accepts one APK transfer at a time, returns HTTP 403 for an incorrect code and HTTP 503 when busy. No endpoint accepts a filename or path. App data and the data-sync server are outside this protocol.
- Sharing is opt-in and lasts up to ten minutes while Peer Updates is visible. Closing the screen, backgrounding it, or choosing **Stop sharing** stops the listener and advertisement. A new session uses a new code.
- Transfer uses local HTTP. Only public package metadata is discoverable without the code. Anyone able to observe LAN traffic can read the code and APK in transit. The receiver derives trust from the actual package identity and signing certificates, and Android makes the final install decision. There is no automatic installation, forwarding or cloud lookup.
- The receiver streams to private cache and verifies exact length and SHA-256, then parses the actual archive and compares all compatibility metadata before staging. Cancellation or a failed transfer removes the partial file. Retrying starts a fresh transfer. Persistent install state records the requested version and Android session, independently of user data.
- The current app targets API 35. If the project raises its target to 37, direct LAN access including manual IPv4 requires Android's new local-network runtime permission. Discovery can also fail on networks that block mDNS or isolate clients. Manual IPv4 works when hosts can connect directly. Wi-Fi Direct, automatic hotspot setup and split APK bundles are outside v1.

## Physical acceptance checklist

1. Install three same-flavor, same-type APKs signed by the same key. Give A a higher version code than B and C. Create recognizable local data on B and C.
2. Disable mobile data and remove the Wi-Fi network's internet uplink. Keep the LAN or a phone hotspot running. Start sharing on A. Discover A on B, then repeat the lookup with manual IP and port.
3. On B check the newer status, try a wrong code, then start the transfer with the valid code. Cancel mid-transfer and confirm a fresh request succeeds. Disconnect Wi-Fi mid-transfer and retry after reconnecting.
4. Deny Android's install-source permission. Verify B retains the verified APK and can request permission again. Grant permission, decline the package installer once, then repeat and approve it. Reopen B and check actual version code and its existing data.
5. Turn A off. Start sharing from B and update C. Verify C preserves its data. Scan from B against C and verify equal version. Compare against an older build and separate prod/exp or signer variants and verify they offer no update.
6. Export from B through the document picker to local storage and USB, and through the system Share chooser. Test the temporary browser URL from a device without ForwardApp. On B, stop sharing, background the screen and allow a session to expire in separate attempts. Verify the URL and discovery stop working. Test a split installation if available and verify its base APK is never offered.
7. For a diagnostic build, check the private download cache after cancellation, hash mismatch and lost connection. Confirm a refused installer session reports a failure after returning to Peer Updates.

Android reference for [PackageInstaller sessions](https://developer.android.com/reference/android/content/pm/PackageInstaller.Session), [NSD](https://developer.android.com/reference/android/net/nsd/NsdManager) and [local-network permissions](https://developer.android.com/privacy-and-security/local-network-permission).
