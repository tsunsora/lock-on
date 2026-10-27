# Releasing Lock On

## Build the release files

Set `JAVA_HOME` to a JDK 21 that includes `jpackage`. On this machine:

```bash
export JAVA_HOME=/e/Coding/energy-tracker/.tools/jdk
```

Then build both release files:

```bash
./gradlew releaseArtifacts
```

That builds both files into `dist/`, which git ignores:

| File | What it is |
| --- | --- |
| `LockOn-<version>-windows.msi` | Windows installer with its own Java runtime. It installs for the current user, so it needs no administrator prompt, and lets you pick the folder. It adds Start-menu and desktop shortcuts. |
| `LockOn-<version>-android.apk` | Android release build, for sideloading. |

It also writes `lock-on-update.json`, the update feed installed apps read. It lists the version, the first paragraph under this version's heading in [CHANGELOG.md](CHANGELOG.md) as release notes, and the SHA-256 of each file.

The first MSI build downloads the WiX toolset automatically.

## Publish an update

Installed apps check `lockon.updateFeed` in [gradle.properties](gradle.properties) once a day while they're open. It points at the **latest** GitHub release of this repo, so:

1. Bump `lockon.version` and `lockon.versionCode`, and add a changelog heading for the version.
2. Run `./gradlew releaseArtifacts`.
3. Create a GitHub release tagged `v<version>` and attach all three files from `dist/`: the MSI, the APK and `lock-on-update.json`.
4. Publish it. Drafts and pre-releases aren't "latest", so apps don't see them until then.

What people see:

- **Windows:** the update downloads in the background and installs when they close Lock On. **Sync → Restart and update** installs it right away. Either way the MSI runs with a progress bar and no questions, into the folder Lock On is already in, and keeps their decks. The install log is in `%APPDATA%\LockOn\updates\install.log`.
- **Android:** the update downloads on Wi-Fi, then **Sync → Install update** hands it to Android. The first time, Android asks them to allow installs from Lock On and to confirm. On Android 12 and later, updates after that usually install without asking.

Both can turn off **Update automatically** under Sync, and then updates only download when they tap **Download**. Each download is checked against the SHA-256 in the feed before it's used.

The feed has to be readable without signing in. While this repo is private, the check fails quietly. Either make the repo public, or publish releases somewhere public (for example a separate releases-only repo) and point `lockon.updateFeed` at its `lock-on-update.json`. File names in the feed are resolved next to the feed, so the three files must sit together.

Android only installs an update signed with the same key as the installed app, which is one more reason to set up the release key (below) before anyone installs a release.

## Version

Change `lockon.version` (and bump `lockon.versionCode` for Android) in [gradle.properties](gradle.properties). Everything else reads from there: the Android version name and code, the installer version, the version shown on the Sync screen, and the version each device reports during sync.

Installer formats need a major version above zero, which is why the first release is `1.0.0`. Keep `upgradeUuid` in [desktopApp/build.gradle.kts](desktopApp/build.gradle.kts) unchanged forever, so a new MSI upgrades an old install in place.

## Android signing

Until a release key is set up, the release APK is signed with the **debug key**, and the build prints a warning. That's fine for testing. Don't publish an APK signed that way: Android only installs updates signed with the same key, so switching keys later forces everyone to uninstall first, which deletes their local decks.

Create a release key once, and keep it and its passwords backed up somewhere safe outside the repo:

```bash
keytool -genkeypair -v -keystore lock-on-release.jks -alias lock-on -keyalg RSA -keysize 4096 -validity 10000
```

Then either create `keystore.properties` in the repo root (git ignores it):

```properties
storeFile=lock-on-release.jks
storePassword=…
keyAlias=lock-on
keyPassword=…
```

or set `LOCKON_KEYSTORE_PATH`, `LOCKON_KEYSTORE_PASSWORD`, `LOCKON_KEY_ALIAS` and `LOCKON_KEY_PASSWORD`.

## What is deliberately off

- **Code shrinking** (R8 on Android, ProGuard on desktop). Room, Ktor, Netty and kotlinx.serialization each need keep rules, and those can only be confirmed by running the shrunk build on real devices. Turn it on with that testing; it would roughly halve the APK.
- **Code signing for the MSI.** Windows SmartScreen warns about unsigned installers ("Windows protected your PC" → *More info* → *Run anyway*). Signing needs a code-signing certificate.
- **macOS and Linux packages.** The build is configured for DMG and DEB, but they have to be built on those systems.

## Before publishing: device checklist

Automated tests cover rules, parsing, revisions, sync, the HTTP routes and the auto-sync timing. These need real hardware:

- [ ] Install the MSI on a clean Windows user account. Launch it, and allow Lock On through Windows Firewall on **Private networks** when asked.
- [ ] Install the APK on a phone on the same Wi-Fi.
- [ ] Pair by QR code, by **Nearby desktops**, and by typing the address.
- [ ] Edit on the phone and see it on the desktop within seconds, then the reverse.
- [ ] Close the desktop app. The phone should show *Waiting for the desktop* with no error. Reopen it, and sync should resume by itself.
- [ ] Edit the same deck on both devices while they can't reach each other. Reconnect, and you should get **Needs review**.
- [ ] Delete a deck on one device. It shouldn't come back after syncing.
- [ ] Turn off Wi-Fi in the middle of a sync, then sync again.
- [ ] Block Lock On in Windows Firewall and check the phone's error message says what to do.
- [ ] Move the desktop to another network so its address changes, then pair again by QR code. Sync should pick up where it left off.
- [ ] Open Lock On twice on Windows. The second launch should say it's already open.
- [ ] Install the new MSI over the old one, and check that decks are kept. Repeat with the APK.
- [ ] Publish a test release with a higher version. On Windows, check that **Restart and update** reopens Lock On on the new version, in a folder other than the default too. On Android, check that **Install update** asks once and then reopens on the new version.
- [ ] Desktop logs are in `%APPDATA%\LockOn\lock-on.log`.
