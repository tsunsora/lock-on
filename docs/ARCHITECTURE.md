# Lock On architecture

How Lock On stores decks and syncs them between devices. For what each screen does, see [UX_MAP.md](UX_MAP.md).

## Data model

The card catalog (`cards`, `printings`) is installed per device from a versioned JSON file and is never synced. Personal data is stored as **deck revisions**:

- Every save writes a revision: a full deck snapshot with a UUID, its parent revision IDs, and the device that made it.
- The `decks` and `deck_entries` tables are a cache, rebuilt from the deck's *head* revisions (revisions that nothing supersedes).
- One head means the deck is resolved. Two or more heads mean two devices edited independently. Both versions are kept, the deck is flagged **Needs review**, and a comparison screen lets you keep one. Your choice is saved as a merge revision whose parents are all the heads.
- Deleting a deck writes a deletion revision. A device that still has an older copy receives that deletion as a newer revision, so it cannot restore the deck. A deletion that crosses an unsynced edit becomes a conflict, so the edit is not lost.
- Revisions are ordered by their parent links, never by timestamps, so clock differences between devices do not matter.
- Autosaves fold into the latest revision until that revision leaves the device, is kept as a version, or sits for 30 minutes. After that, revisions are immutable. Saving unchanged content adds nothing. This makes the revision history the deck's version history.
- Folders, the cover card and the match log are part of each snapshot, so they sync like any other edit. A folder is only a name on its decks. Settling a conflict keeps the chosen version but takes matches from every head, deduplicated by match ID.

Ruleset checks: a 50-card main deck, a 10-card resource deck, at most two colors, and at most four copies per card number, with alternate art counting toward the same card number. Resource cards are unlimited unless the ruleset sets `resourceMaxCopies`. Rulesets can also ban cards, restrict cards to fewer copies, and ban combinations of cards.

## Sync protocol (v1)

The desktop hosts HTTPS on port **53340** (or the next free port). Phones start every exchange.

1. **Pairing, LocalSend-style.**
   - Every device announces itself over DNS-SD with a role (`desktop` or `phone`).
   - **Tap a nearby computer** on the phone: the phone sends a pair request with a random nonce and no code.
   - **Invite a nearby phone** on the computer: the phone holds `GET /v1/invites?device=<id>` open while it's nearby, shows *"… wants to connect"*, and on Connect sends the same request carrying the invitation ID.
   - Either way the computer asks its owner to confirm. Both screens show a 6-digit number taken from SHA-256 of the computer's certificate fingerprint (as each side sees it) plus the nonce. An impostor's request, or an interception with another certificate, shows a different number, so comparing the two screens authenticates both sides, like Bluetooth's numeric comparison.
   - A device that was just declined waits a minute before it can ask again.
   - The original flow below still works.
1. **Pairing by QR code or address.** The desktop shows a QR code: `lockon://pair?host=…&port=…&code=…&fp=<sha256 of its certificate>`. The phone connects, and its TLS client accepts only that exact certificate. The phone sends the 6-character code, and the desktop asks you to confirm, showing a verification code that the phone also shows. When you confirm, the phone receives a random token. The desktop stores only the token's SHA-256 hash. The phone stores the token encrypted with an Android Keystore key and pins the certificate fingerprint.
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
4. **Data format.** Deck snapshots are at data format **3**: format 2 added the Considering section, and format 3 added folders, cover cards and the match log. Both sides must use the same format to sync; otherwise the phone shows which device to update. An older app would drop the newer fields the next time it saved a deck.

## Key files

- Construction rules: [DeckValidator.kt](../shared/src/commonMain/kotlin/app/lockon/rules/DeckValidator.kt), with rulesets in [rulesets.json](../shared/src/commonMain/composeResources/files/rulesets/rulesets.json)
- Revisions and conflicts: [DeckRepository.kt](../shared/src/commonMain/kotlin/app/lockon/data/DeckRepository.kt), [RevisionGraph.kt](../shared/src/commonMain/kotlin/app/lockon/sync/RevisionGraph.kt)
- Sync protocol: [Protocol.kt](../shared/src/commonMain/kotlin/app/lockon/sync/Protocol.kt), [SyncEngine.kt](../shared/src/commonMain/kotlin/app/lockon/sync/SyncEngine.kt), [SyncServer.kt](../desktopApp/src/main/kotlin/app/lockon/desktop/SyncServer.kt)
- App updates: [AppUpdater.kt](../shared/src/commonMain/kotlin/app/lockon/update/AppUpdater.kt), [WindowsUpdateInstaller.kt](../desktopApp/src/main/kotlin/app/lockon/desktop/WindowsUpdateInstaller.kt), [AndroidUpdateInstaller.kt](../androidApp/src/main/kotlin/app/lockon/android/AndroidUpdateInstaller.kt)
