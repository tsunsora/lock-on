# Lock On

A Gundam Card Game deck builder for Android and Windows (macOS and Linux later). The two apps work on their own and sync decks directly over your local network: no account, no cloud server.

## What's in this first milestone

- **Card browser**: search by name, card number or text, filter by color, type, level, cost, set and trait, sort, and switch between list and grid views. Card details show corrected text and every printing.
- **Deck editor**: add and remove cards with one tap, set the preferred printing, write notes. It saves automatically. Cards are tagged **Max** at their copy limit and **Off-color** when they would add a third color, and **Deck colors only** hides the rest. The deck is grouped by type. Wide windows show search, the deck and analysis side by side; phones use tabs (Deck, Add cards, Analysis, Test).
- **Considering list**: park cards you're weighing up in a separate section that never counts toward deck size, colors or legality. Move copies in and out with one tap. It syncs and exports with the deck (`// Considering`; `Maybe:` and `Sideboard` headers also import into it).
- **Test hand**: practice opening hands with the official setup: draw 5, redraw once to the bottom, set 6 shields, then draw each turn.
- **Deck library**: create, duplicate, rename, tag, search and filter decks. Deleting shows **Undo** instead of asking first.
- **Deck analysis**: legality messages, Burst and Blocker counts, Unit–Pilot link pairs, level and cost curves, and card-type and color counts. An incomplete deck saves as a *Draft*.
- **Sync**: desktop QR code pairing (with a manual address and code as a fallback), paired devices, last successful sync, and **Sync now**.
  - **Nearby desktops**: phones find Lock On desktops on the same network and pair with just the code, no address to type.
  - **Auto-sync while open**: with both apps open on the same network, edits reach the other device within seconds, and a paired desktop that changes IP address is followed automatically. It runs only while the phone app is on screen. Nothing runs in the background.
- **Import and export**: plain-text deck lists (paste, copy or file) and a versioned JSON backup.
- **Card catalog updates**: install a catalog from a file or an `https://` link, and later check that link for a newer version. Every install shows a preview first, including any cards your decks use that the new catalog lacks. Decks are never changed. The format is in [docs/CATALOG_FORMAT.md](docs/CATALOG_FORMAT.md), with a CSV converter in `scripts/`.
- **Card artwork**: when a catalog's printings include `artworkUrl` links, card lists, grids, test hands and card details show the images. They're cached on each device, never synced, and can be turned off under **Cards → Catalog**. Until an image loads, or if its link breaks, the colour placeholder stays.

The built-in catalog holds **20 fictional sample cards** (`SMP-001`…`SMP-020`). They follow the shape of Gundam Card Game data but contain no official text or artwork. Before importing the real catalog, find a data source you are allowed to use, and get permission to distribute card text and art. The official site has no public API.

## UX

- [docs/UX_MAP.md](docs/UX_MAP.md): every screen, what it shows, and how they connect.
- [docs/UX_RULES.md](docs/UX_RULES.md): the rules every screen follows, with the research behind them. Inspiration came from Stella Sora and Aniimo, and from comparing other Gundam Card Game builders (ExBurst, GNDM TCG Dex, TCG Stacked).

Desktop shortcuts: `Ctrl+F` searches cards, and `Esc` goes back.

## Project layout

| Module | Contents |
| --- | --- |
| `shared` | Kotlin Multiplatform (Android + JVM): models, rules, Room database, repositories, sync engine, Ktor sync client, and all Compose screens |
| `androidApp` | Android entry point, QR scanner, certificate-pinned OkHttp client, Keystore-encrypted token storage, file pickers |
| `desktopApp` | Desktop entry point, embedded HTTPS Ktor server, TLS identity, pairing confirmation, QR rendering |

Key files:

- Construction rules: [DeckValidator.kt](shared/src/commonMain/kotlin/app/lockon/rules/DeckValidator.kt), with rulesets in [rulesets.json](shared/src/commonMain/composeResources/files/rulesets/rulesets.json)
- Revisions and conflicts: [DeckRepository.kt](shared/src/commonMain/kotlin/app/lockon/data/DeckRepository.kt), [RevisionGraph.kt](shared/src/commonMain/kotlin/app/lockon/sync/RevisionGraph.kt)
- Sync protocol: [Protocol.kt](shared/src/commonMain/kotlin/app/lockon/sync/Protocol.kt), [SyncEngine.kt](shared/src/commonMain/kotlin/app/lockon/sync/SyncEngine.kt), [SyncServer.kt](desktopApp/src/main/kotlin/app/lockon/desktop/SyncServer.kt)

## Data model

The card catalog (`cards`, `printings`) is installed per device from a versioned JSON file and is never synced. Personal data is stored as **deck revisions**:

- Every save writes a revision: a full deck snapshot with a UUID, its parent revision IDs, and the device that made it.
- The `decks` and `deck_entries` tables are a cache, rebuilt from the deck's *head* revisions (revisions that nothing supersedes).
- One head means the deck is resolved. Two or more heads mean two devices edited independently. Both versions are kept, the deck is flagged **Needs review**, and a comparison screen lets you keep one. Your choice is saved as a merge revision whose parents are all the heads.
- Deleting a deck writes a deletion revision. A device that still has an older copy receives that deletion as a newer revision, so it cannot restore the deck. A deletion that crosses an unsynced edit becomes a conflict, so the edit is not lost.
- Revisions are ordered by their parent links, never by timestamps, so clock differences between devices do not matter.
- Autosaves fold into the latest revision until that revision leaves the device. After that, revisions are immutable.

Ruleset checks: a 50-card main deck, a 10-card resource deck, at most two colors, and at most four copies per card number, with alternate art counting toward the same card number. Resource cards are unlimited unless the ruleset sets `resourceMaxCopies`. Rulesets can also ban cards, restrict cards to fewer copies, and ban combinations of cards.

## Sync protocol (v1)

The desktop hosts HTTPS on port **53340** (or the next free port). Phones start every exchange.

1. **Pairing.** The desktop shows a QR code: `lockon://pair?host=…&port=…&code=…&fp=<sha256 of its certificate>`. The phone connects, and its TLS client accepts only that exact certificate. The phone sends the 6-character code, and the desktop asks you to confirm, showing a verification code that the phone also shows. When you confirm, the phone receives a random token. The desktop stores only the token's SHA-256 hash. The phone stores the token encrypted with an Android Keystore key and pins the certificate fingerprint.
   - Codes expire after 10 minutes, rotate after each pairing, and rotate after 5 wrong guesses.
   - Manual entry (address, port, code) works without the fingerprint. The phone trusts the first certificate it sees, but only after you compare the verification code on both screens.
2. **Sync.** `GET /v1/hello` checks protocol and data-format compatibility first. Then the phone pushes the revisions the desktop hasn't seen (`POST /v1/sync/push`) and pulls new revisions from the desktop (`GET /v1/sync/pull?after=<cursor>`), 200 at a time.
   - Each device keeps per-peer cursors in its own insertion order.
   - Every batch is applied in one database transaction, together with its cursor update. Revision IDs are unique, so a retried or replayed batch is ignored.
   - An interrupted sync resumes where it stopped.
   - The database file itself is never transferred, and card images are never part of sync.
3. **Discovery and live sync.**
   - The desktop advertises `_lockon._tcp` over DNS-SD (mDNS) with its device ID, name, certificate fingerprint, and protocol and data versions.
   - Phones browse for it only while the app is in the foreground. A discovered desktop is just a hint; trust still comes from pairing.
   - While open, the phone holds `GET /v1/sync/wait?after=<cursor>` open, and the desktop answers within 25 seconds, as soon as it has new revisions. Local edits trigger a sync about 2 seconds after they settle.
   - Failures back off from 5 to 60 seconds and show as "Waiting for the desktop", not as errors.
4. **Data format.** Deck snapshots are at data format **2**, which added the Considering section. Both sides must use the same format to sync; otherwise the phone shows which device to update. A format-1 app would drop considered cards the next time it saved a deck.

## Building

You need JDK 17 or later and the Android SDK (platform 36). On this machine both come from `energy-tracker/.tools`, and `local.properties` points `sdk.dir` there.

```bash
export JAVA_HOME=/e/Coding/energy-tracker/.tools/jdk
```

Run the desktop app:

```bash
./gradlew :desktopApp:run
```

Build the Android debug APK (output: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`):

```bash
./gradlew :androidApp:assembleDebug
```

Run all tests:

```bash
./gradlew :shared:jvmTest :desktopApp:test
```

Build the Windows installer and the release APK into `dist/` (see [RELEASING.md](RELEASING.md) for signing and the device checklist):

```bash
./gradlew releaseArtifacts
```

Running a second desktop profile, for example to test sync against yourself, uses a separate data folder:

```bash
./gradlew :desktopApp:run -Plockon.dataDir=/path/to/profile
```

Desktop data lives in `%APPDATA%\LockOn` on Windows, `~/Library/Application Support/LockOn` on macOS, and `~/.local/share/lock-on` on Linux. Deleting `tls.p12` and `tls.key` there gives the desktop a new identity, and every phone must pair again.

`desktopApp:test` also renders the main screens to `desktopApp/build/screenshots/` for layout review.

## Testing checklist for real devices

Covered by automated tests:

- the construction rules and deck-list parsing
- the revision graph
- end-to-end sync between two databases: round trips, folding autosaves, concurrent edits, delete against edit, interrupted pulls, replayed batches, and incompatible versions
- pairing and sync through the real HTTP routes, including wrong codes, declined pairing and invalid tokens

Still to do by hand:

- Pair a real phone with the desktop on Wi-Fi, by QR code and by manual entry.
- Sync, edit on the desktop, and sync back.
- Allow the Windows Defender Firewall prompt on first launch, using **Private networks**. Then test with the firewall blocking Lock On to see the error message.
- Turn off Wi-Fi in the middle of a sync, then sync again.
- Change the desktop's IP address (another network), then re-scan the QR code. Re-pairing keeps the sync progress.

## Next milestones

- A licensed source for the real card data and artwork, published as a catalog file.
- A release signing key, code-signed installers, and shrunk builds (see [RELEASING.md](RELEASING.md)).
- Database migration tests once the schema changes (`shared/schemas` holds the exported Room schema).
