# Lock On UX map

How screens connect, what each one is for, and what you can do there. The rules every screen follows are in [UX_RULES.md](UX_RULES.md).

## Map

```mermaid
flowchart LR
    subgraph Top["Top level (floating tray on desktop, floating dock on phones)"]
        D[Decks]
        C[Cards]
        S[Sync]
    end

    D -->|tap deck| E[Deck editor]
    D -->|New deck| E
    D -->|Import| IMP{{Import sheet}}
    IMP -->|deck list| E
    D -->|deck menu| DM{{Rename / Move to folder / Tags / Duplicate / Export / Delete}}
    DM -->|Delete| UNDO([Snackbar: Undo])
    D -->|New folder| NF{{Name a folder and pick its decks}}
    D -->|"Needs review" deck| X[Compare versions]

    E -->|card body| CD{{Card details}}
    E -->|Export| EXP{{Export: List / Image / Print sheet}}
    E -->|Versions: older version| VC{{Compare with now, restore cards}}
    E -->|conflict banner| X
    X -->|Keep a version| E

    C -->|card body| CD

    S -->|desktop| QR[Pair a phone: QR + code]
    S -->|phone| SC[Scan QR / Enter address]
    QR --> CONF{{Confirm pairing dialog}}
    SC --> VER{{Verification code dialog}}
```

Rectangles are screens. Hexagons are sheets or dialogs that sit over the current screen. N2 forbids a third screen level, so anything deeper must be one of these.

## Screens

### Decks (top level)
- **Purpose:** find a deck and start working on it.
- **Shows:** each deck as a tile with its cover card (the one picked in the editor, else its highest-level Unit, or a card back for an empty deck), a color-identity band, name, Legal / Draft / Needs review tag, `Main n/50`, its folder, tags, win/loss record, and when it was last edited. On desktop the tiles form a grid.
- **Folders:** a list beside the decks on desktop (**All decks**, each folder with its count, **No folder**, **New folder**), a chip row on phones. A folder is a filter, not a screen (N2).
  - **New folder** names the folder and picks its decks in one step, since a folder shows only while it holds a deck.
  - An open folder shows **Rename** and **Remove folder**. Removing only takes its decks out, with Undo.
  - **New deck** inside a folder puts the deck in it.
  - Folders sync with the decks.
- **Actions:**
  - Bottom right: **New deck**.
  - Top: **Import** and **Back up all**.
  - Search decks by name, and filter by folder and tag.
  - Each tile's menu: Rename, Move to folder (pick a folder in use, or type a new one), Edit tags, Duplicate (keeps folder and cover, not matches), Export, Delete (with Undo).
- **Empty state:** explains the three ways to get a deck: create one, import one, or pair a device.

### Deck editor (level 2)
- **Header:**
  - name (tap to rename), color pips, legality tag, `Main n/50`
  - **Export**
  - a conflict banner when one exists
- **Panes:** wide windows show two areas, the card list and a deck panel with **Deck · Analysis · Test · Matches · Versions** tabs. Phones show them in a bottom bar (**Deck · Add · Analysis · Test · More**, with Matches and Versions under More):
  1. **Add cards:** catalog search with filters, as a grid of whole cards (3 across on phones), with the deck count, outline and − / + under each card.
     - Each card shows its in-deck count and − / +, and gets **Max** at the copy limit and **Off-color** when it would add a third color.
     - **Deck colors only** narrows the list to the deck's colors.
     - Sort by number, level, cost or name.
  2. **Deck:** grouped by type (Units, Pilots, Commands, then Bases), each with a HUD header and count, sorted by level and then cost, with − / + on every row.
     - **Considering** comes after the deck: cards you're weighing up, never counted toward size, colors or legality. Each row has **To deck** (moves one copy in) and − (drops one).
     - Card details offer **Consider**, **Move 1 to Considering** or **Move 1 to deck**, depending on where the card already is.
     - In the card list, a card that is only being considered is tagged **Considering**.
     - Card details also offer **Use as deck cover**.
     - Notes go at the end.
  3. **Analysis:**
     - legality messages first
     - Gundam readouts: Burst cards (how many can trigger from shields), Blockers, and Unit–Pilot link pairs present
     - level curve, cost curve, type and color counts
  4. **Test (sample hand):**
     - Shuffle and draw 5, with one **Redraw** that follows the official rule.
     - The next 6 cards become shields, face down until you reveal them.
     - **Draw** adds one card per turn.
     - **New game** shuffles again.
     - Stats show how many Units of level 2 or lower are in hand.
  5. **Matches (win/loss log):**
     - record, win rate, and results going first and second
     - **Log a match:** an optional opponent (picked from earlier opponents or typed), turn order and note, then **Win**, **Loss** or **Draw** logs it in one tap
     - results by opponent, then every game, newest first, each removable with Undo
     - Matches sync. When a conflict is settled, games logged on every device are kept.
  6. **Versions:**
     - Every version newest first, with time, device, counts and what changed (`+3 −1 cards · renamed`).
     - Edits join the current version until you choose **Keep as a version**, it syncs, or the deck sits for 30 minutes.
     - Opening an older version lists its card differences from now, and **Restore these cards** brings them back with Undo. Name, notes, folder and matches stay as they are.
- **Export dialog:** **List** (copy or save the text list), **Image** (a 2160-pixel-wide picture of the main deck, five cards a row, with its cover, colors, legality, curve, type counts and a QR code holding the list, saved as PNG), and **Print sheet** (a deck registration sheet saved as a web page to print from any browser).
- **Leaving:** back or the navigation, which saves first.

### Compare versions (level 2)
- **Purpose:** settle a deck that was edited on two devices before they synced.
- **Shows:** one card per version, with the device, time, name, counts and per-card differences.
- **Action:** **Keep this version**, or **Delete the deck** if one side deleted it. This returns to the editor.

### Cards (top level)
- **Purpose:** browse and learn cards outside any deck.
- **Shows:** a search field with the card count, then one chip row (results, Filters, Sort, Grid/List, colors, types), as a gallery of whole cards or a list. On desktop, tapping a card opens its details in a side panel; on phones, in a dialog.
- **Card details:** artwork when the catalog has it (tap a printing to switch art), type and stats, corrected text, traits, zones, link, and printings.
- When a weekly background check finds a newer official list, the **Catalog** button reads *Card database update available* (UX rule C1: no pop-up).
- **Catalog** (button next to the catalog name) opens a dialog that:
  - shows the installed catalog: name, version, card count, where it came from, and when it was installed
  - has a **Show card artwork** switch (images are cached per device and never sync)
  - **Official card list:** **Download the latest card database** (gcg-api, converted on the device), then **Check for updates** / **Download update**, with the gcg-api credit underneath
  - **Other sources:** an `https://` link to **Download**, then later **Check for update**, or **Import file**
  - offers **Go back to the sample catalog**
  - always shows a preview before installing: an update or a replacement, warnings, and any cards your decks use that the new catalog lacks. **Update** / **Replace catalog** confirms, since this can't be undone (C2).

### Sync (top level)
- **Desktop:**
  - **Nearby phones:** every phone with Lock On open on the network, each with **Connect** (an invitation the phone accepts) or *Waiting for the phone… Cancel*. Paired phones are marked.
  - **Other ways to connect:** the QR code, address, port, pairing code and code expiry, shown by default only until a phone is paired
  - paired phones with their last successful sync, and *Connected now* while a phone's auto-sync is live
  - a dialog to confirm a pairing request: *"Pixel wants to connect"* or *"Pixel accepted"*, with the 6-digit number to compare
- **Phone:**
  - **Auto-sync while Lock On is open** switch (on by default), once a desktop is paired
  - **Nearby computers** found on the network, each with **Connect** (no code to type; the computer confirms after comparing the 6-digit number)
  - *"[Computer] wants to connect"* when a computer invites this phone, with **Connect** or **Not now**
  - **Other ways to connect** (collapsed): **Scan QR code**, or address, port and an optional code
  - paired desktops with **Sync now**, last sync, the last manual-sync error, and a live line: *Live: changes sync automatically* or *Waiting for the desktop*
  - a verification code dialog while the desktop is confirming
- **Always:** the line "Only decks sync. No account or cloud server."

## Primary journeys

1. **First deck on a phone:**
   1. Decks → **New deck** → name it.
   2. Editor opens on **Add cards**.
   3. Tap + until the header reads `Main 50/50`.
   4. Switch to **Test** to try hands.
   5. Back. It's already saved.
2. **Import a list from a friend:**
   1. Decks → **Import** → paste.
   2. The editor opens on the imported deck, with any unreadable lines reported.
3. **Phone to computer (first time), LocalSend-style:**
   1. Open Lock On on both, on the same Wi-Fi. Each appears in the other's **Nearby** list.
   2. Tap **Connect** on either device. If the computer started it, tap **Connect** on the phone too.
   3. The computer shows a 6-digit number, and so does the phone. If they match, click **Pair**.
   4. Sync starts at once and stays automatic while both are open.
   - Fallback when discovery is blocked:
   1. Desktop: **Settings**, which shows the QR code.
   2. Phone: **Settings** → **Scan QR code**.
   3. Desktop: confirm the codes match → **Pair**.
   4. The phone syncs right away.
   5. With both open, edits on either device appear on the other within seconds. **Sync now** is there when auto-sync is off.
   - Or, without the camera: the desktop shows under **Nearby desktops** on the phone → **Pair** → type the code.
4. **Edited on both devices:**
   1. The deck tile shows **Needs review**.
   2. Open **Compare versions** → **Keep this version** → back in the editor.
5. **Remove a deck by mistake:** Delete → **Undo** in the snackbar.
6. **Not sure about a card:**
   1. Open the card → **Consider**, or **Move 1 to Considering** if it's already in the deck.
   2. It waits under Considering without affecting legality.
   3. **To deck** brings a copy back in.
   4. Exported lists carry a `// Considering` section.
