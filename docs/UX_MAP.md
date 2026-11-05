# Lock On UX map

How screens connect, what each one is for, and what you can do there. The rules every screen follows are in [UX_RULES.md](UX_RULES.md).

## Map

```mermaid
flowchart LR
    subgraph Top["Top level (floating tray on desktop, floating dock on phones)"]
        D[Decks]
        C[Cards]
        S[Sync]
        ST[Settings]
    end

    D -->|tap deck| E[Deck editor]
    D -->|New deck| E
    D -->|New deck: paste a list| IMP{{Reads the list, opens the editor}}
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
- **Shows:** each deck as a tile with its cover card (the one picked in the editor, else its highest-level Unit, or a card back for an empty deck) on a band in the deck's colours, its name, `n/50` gauge, Legal / Draft / Needs review tag and win/loss record. On desktop the tiles form a grid four or more across; phones keep a row with the cover beside the name, plus the folder.
- **Folders:** a list beside the decks on desktop (**All decks**, each folder with its count, **No folder**, **New folder**), a chip row on phones. A folder is a filter, not a screen (N2).
  - **New folder** names the folder and can put decks in it in the same step; picking none makes an empty folder. A folder stays, even empty, until it is removed.
  - **Rename** and **Remove folder** are on the folder: its ⋮ button on hover or a right-click in the desktop list, a right-click on a phone chip, and the **More actions** menu while the folder is open. Removing asks first, says how many decks it holds, then only takes its decks out, with Undo.
  - **New deck** inside a folder puts the deck in it.
  - Drag a deck onto a folder, or onto **No folder**, to move it, with Undo. A mouse drags at once; a finger holds the deck first so the list still scrolls. Dropping on **New folder** opens it with that deck ticked.
  - Folders sync with the decks.
- **Actions:**
  - Beside search on desktop, a round **+** at the bottom right on phones: **New deck**.
  - **New deck** names the deck, picks a folder (or a new one), and starts **Empty** or from **Paste a list**, which reads the list as you type, opens a file, and also takes a Lock On backup. **Back up all decks** is in Settings.
  - Search decks by name (Ctrl+F on a computer), sort by **Last edited**, **Name** or **Win rate**, and filter by folder and tag.
  - Each tile's menu: Rename, Move to folder (pick a folder in use, or type a new one), Edit tags, Duplicate (keeps folder and cover, not matches), Export, Delete (with Undo).
- **Empty state:** explains the three ways to get a deck: create one, import one, or pair a device.

### Deck editor (level 2)
- **Header:**
  - name (tap to rename), color pips, legality tag, `Main n/50`
  - **Export**
  - a conflict banner when one exists
- **Panes:** wide windows show two areas, the card list and a deck panel: the gauge, **Export** and **More** (Edit as text, Card view or List view) above **Deck · Analysis · Test · Matches · Versions** tabs. Phones show them in a bottom bar built around building the deck: **Deck · Add · More**, with Analysis, Test, Matches, Versions, Export and Edit as text under More, which opens them as a two-row grid of tiles just above the bar (Settings › Deck editor can leave any of them out):
  1. **Add cards:** catalog search with filters, as a grid of whole cards (3 across on phones), with the deck count, outline and − / + under each card.
     - **Deck · Side deck** beside the search picks where + and dropped cards go.
     - Each card shows its in-deck count and − / +, and gets **Max** at the copy limit and **Off-color** when it would add a third color. Adding to the side deck, the counts are side deck copies, and a card already in the deck is tagged **Deck n**.
     - **Deck colors** narrows the list to the deck's colors.
     - Sort by number, level, cost or name.
  2. **Deck:** grouped by type (Units, Pilots, Commands, then Bases), each with a HUD header and count, sorted by level and then cost, with − / + on every row.
     - On phones a fixed bar above the list holds the level curve (opens Analysis), a chip per section with its count that jumps to it, **List / Cards**, and the first legality problem. An empty deck offers **Add cards**.
     - The **Side deck** comes after the deck, never counted toward size, colors or legality. Each row has **To deck** (moves one copy in) and − / +.
     - Card details have a side deck − / +, and **Move 1 to side deck** or **Move 1 to deck**, depending on where the card already is.
     - In the card list, a card with side deck copies is tagged **Side n**.
     - Card details also offer **Use as deck cover**.
     - Notes go at the end.
  3. **Analysis:**
     - what keeps the deck from being legal, only while something does
     - **Burst**, **Blockers** and **Links** as three tiles; **Links** opens the Unit–Pilot pairs
     - **Level** and **Cost** curves
     - one bar split by type with a legend, and one split by color
  4. **Test (sample hand):**
     - Shuffle and draw 5, held in a fan, with cards left and the turn above it. The card under the pointer comes to the front.
     - **Redraw** follows the official rule, once.
     - 6 shields wait face down; **Reveal** turns them over.
     - **Draw** adds one card per turn; the first draw keeps the hand and sets the shields.
     - **New game** shuffles again.
  5. **Matches (win/loss log):**
     - win rate as a ring, the record, and a bar each for going first and second
     - **Log a match:** an optional opponent (picked from earlier opponents or typed), **1st** or **2nd**, a note behind the pencil, then **Win**, **Loss** or **Draw** logs it in one tap
     - a bar per opponent, then every game, newest first; tapping one shows its time and removes it with Undo
     - Matches sync. When a conflict is settled, games logged on every device are kept.
  6. **Versions:**
     - **Now** on top, with what has changed so far and **Keep as a version**.
     - Every older version newest first, with its device as an icon, time, card count, cards added and taken away, and **Renamed** or **Created**.
     - Edits join the current version until you choose **Keep as a version**, it syncs, or the deck sits for 30 minutes.
     - Opening a version shows in place what restoring would change, and **Restore these cards** brings them back with Undo. Name, notes, folder and matches stay as they are.
- **Export dialog:** **List** (copy or save the text list), **Image** (a 2160-pixel-wide picture of the main deck, five cards a row, with its cover, colors, legality, curve, type counts and a QR code holding the list, saved as PNG), and **Print sheet** (a deck registration sheet saved as a web page to print from any browser). On wide windows the preview sits beside the controls and shows the whole image at once (**View: Full width** zooms it to scroll); on phones the dialog fills the screen and the preview takes every row the controls leave.
- **Leaving:** back or the navigation, which saves first.

### Compare versions (level 2)
- **Purpose:** settle a deck that was edited on two devices before they synced.
- **Shows:** one card per version, with the device, time, name, counts and per-card differences.
- **Action:** **Keep this version**, or **Delete the deck** if one side deleted it. This returns to the editor.

### Cards (top level)
- **Purpose:** browse and learn cards outside any deck.
- **Shows:** a search field with the card count, then one chip row (results, Filters, Sort, Grid/List, colors, types), as a gallery of whole cards or a list. On desktop, tapping a card opens its details in a side panel; on phones, in a dialog.
- **Card details:** artwork when the catalog has it (tap a printing to switch art), type and stats, corrected text, traits, zones, link, and printings.
- The catalog is managed from **Settings → Card database**, not from here, so the header has no actions.

**Settings → Card database** shows the installed catalog and, when a weekly background check finds a newer official list, a quiet *Card database update available* line (UX rule C1: no pop-up). **Manage card database** opens a dialog that:
  - shows the installed catalog: name, version, card count, where it came from, and when it was installed
  - has a **Show card artwork** switch (images are cached per device and never sync)
  - **Official card list:** **Download card database** (gcg-api, converted on the device), then **Check for updates** / **Download update**, with the gcg-api credit underneath
  - **Other sources:** an `https://` link to **Download**, then later **Check for update**, or **Import file**
  - offers **Restore sample**
  - always shows a preview before installing: an update or a replacement, warnings, and any cards your decks use that the new catalog lacks. **Update** / **Replace catalog** confirms, since this can't be undone (C2).

### Sync (top level)
Two pages: **Sync devices** (first) and **Send decks**.
- **Sync devices** (both devices):
  - this device's name with a slowly turning beacon: it can be found on the same Wi-Fi
  - **Your devices:** paired devices, each with last sync or the last error, a live line while it keeps merging (*Merging automatically* or *Waiting for the desktop*), and **Sync**. A running sync shows a bar and *Making this phone match Desk…*; then *This phone now matches Desk: 2 decks added, 1 deck removed, settings.* in gold with **OK**, or why it failed.
  - **Sync** opens *Sync with Desk*: **Make this phone match Desk** (first, tinted), **Make Desk match this phone**, then under *Or keep both* **Merge both ways**. On a phone, a **Keep merging** switch per desktop (off for a newly paired one, so it can be matched first). **Forget device** is here too.
  - Either match asks *Replace everything on Desk?*, naming what is replaced and that decks only there are removed, with **Replace** or **Cancel** (C2).
  - **Nearby:** devices not paired yet, each *Tap to pair and sync*; once paired, the choice above opens by itself. A desktop shows *Waiting for Pixel to accept pairing…* with **Cancel** while it waits.
  - **+** opens other ways to connect: the desktop's QR code, address, port and pairing code, or on a phone **Scan QR code** and address entry.
  - A sync started on the desktop waits for the phone (*Waiting for Pixel… Open Lock On on it.*, with **Cancel**). A phone asked to match the desktop shows *Make this phone match Desk?* with **Replace** or **Not now**; other requests run without asking.
- **Send decks** (both devices), LocalSend-style:
  - **Choose decks** opens a checklist of decks. The chosen names show on the card, with **Change**.
  - Every Lock On device nearby is listed, paired or not (phones on a computer, computers on a phone), each with **Send**.
  - While the other device decides, the row reads *Waiting for Pixel to accept…* with **Cancel**. Then it reads *Pixel saved 2 decks.*, *Pixel declined.*, or why it failed.
  - The receiver sees *"Desk wants to send you 2 decks"*, listing each deck and its main deck count, with **Accept** or **Decline**. Accepted decks are added as new decks with their cards, notes, tags and cover. Folders and match logs stay with the sender.
- **Pairing dialogs:** on the desktop, *"Pixel wants to pair"* or *"Pixel accepted"* with the 6-digit number to compare; on a phone, *"[Computer] wants to pair"* when a computer invites it, and the number while the computer confirms.
- Outside this screen, a dot on the Sync tab marks a failed sync, and a snackbar such as *Up to date with Pixel 8: got 3 changes.* follows any sync that moved changes (C1).

### Settings (top level, beside Sync)
- **Purpose:** this device's preferences. Merging leaves them alone; making this device match another copies that device's over.
- **Layout:** each group is a card of rows: the setting's name on the left, its control at the end. Choices of a few options are sliding segmented controls. Wide windows show two centred columns; phones stack the groups and put segmented choices under their names.
- **General:** **Open on** (Decks · Cards · Last screen), **Device name** with **Rename** (used from the next launch), **Welcome tour** with **Show again**.
- **Appearance:** **Theme** (System · Sky · Twilight), **Interface size** (Small · Default · Large · Larger, scaling everything together), **Animations** (System · Reduced · Full), **Show card artwork**.
- **Deck editor:** on phones and in narrow windows, **Bar order** and **In the More menu** (a grid of the sections, at least one stays); always the *Deck colors only* switch; in wide windows, **Reset** for the divider between cards and deck.
- **Data:** **Card database** with **Manage**, the weekly card list check, **Back up all decks**.
- **About:** the version and app updates, last.

## Primary journeys

1. **First deck on a phone:**
   1. Decks → **New deck** → name it.
   2. Editor opens on **Add cards**.
   3. Tap + until the header reads `Main 50/50`.
   4. Switch to **Test** to try hands.
   5. Back. It's already saved.
2. **Import a list from a friend:**
   1. Decks → **New deck** → **Paste a list**.
   2. The editor opens on the imported deck, with any unreadable lines reported.
3. **Phone to computer (first time), LocalSend-style:**
   1. Open Lock On on both, on the same Wi-Fi. Each appears in the other's **Nearby** list.
   2. Tap **Connect** on either device. If the computer started it, tap **Connect** on the phone too.
   3. The computer shows a 6-digit number, and so does the phone. If they match, click **Pair**.
   4. Sync starts at once and stays automatic while both are open.
   - Fallback when discovery is blocked:
   1. Desktop: **Sync**, which shows the QR code.
   2. Phone: **Sync** → **Scan QR code**.
   3. Desktop: confirm the codes match → **Pair**.
   4. The phone syncs right away.
   5. With both open, edits on either device appear on the other within seconds. **Sync now** is there when auto-sync is off.
   - Or, without the camera: the desktop shows under **Nearby desktops** on the phone → **Pair** → type the code.
4. **Edited on both devices:**
   1. The deck tile shows **Review** on wide windows and **Needs review** on phones.
   2. Open **Compare versions** → **Keep this version** → back in the editor.
5. **Give a friend a deck:**
   1. Both open Lock On on the same Wi-Fi and go to **Sync**.
   2. **Choose decks**, tick the decks, **Done**.
   3. Tap **Send** beside the friend's device. No pairing needed.
   4. The friend taps **Accept**, and the decks appear in their library as new decks.
6. **Remove a deck by mistake:** Delete → **Undo** in the snackbar.
7. **Build a side deck:**
   1. Switch the card list to **Side deck**, then + or drag cards in. Or open a card → side deck +, or **Move 1 to side deck** if it's already in the deck.
   2. It waits under Side deck without affecting legality.
   3. **To deck** brings a copy into the deck.
   4. Exported lists carry a `// Side deck` section; older lists with `// Considering` read back into it.
