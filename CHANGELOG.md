# Changelog

## 1.0.0 (2026-10-29)

First release for Android and Windows.

**Deck building**
- Card browser with search by name, number and text, and filters for color, type, level, cost, set and trait. Sort it, and switch between list and grid.
- Deck editor with one-tap add and remove, and autosave. Cards are tagged **Max** at their copy limit and **Off-color** when they would add a third color, and **Deck colors only** hides the rest. The deck is grouped by card type.
- **Side deck** beside each deck. Set the card list to **Add to: Side deck** and add cards with + or by dragging, or move copies between deck and side deck from card details. It never counts toward the deck.
- The resource deck isn't tracked: every player brings 10 standard resources, so decks list only their 50 main-deck cards and resource cards stay out of the editor's card list.
- On desktop, resting the pointer on a card opens the whole card beside it, at a size you can read.
- Analysis:
  - legality messages
  - Burst and Blocker counts, and Unit–Pilot link pairs
  - level and cost curves, and type and color counts
- **Test hand** using the official setup: draw 5, redraw once, set 6 shields, then draw.
- Deck library with tags, search, duplicate, rename, and delete with **Undo**.
- **Deck folders**: a folder list beside the decks on desktop and folder chips on phones. Move decks between folders from the menu or by dragging a deck onto a folder, create a folder with its decks in one step, and rename or remove folders. Folders sync.
- Pick any card as a deck's **cover**.
- **Match log** per deck: wins, losses and draws with optional opponent, turn order and notes, and a record with win rate, going first and second, and results by opponent.
- **Version history**: every version of a deck with what changed, a comparison with the deck now, and restoring an older version's cards.
- Export a deck as an **image** (PNG) or a printable **deck registration sheet**, as well as a text list, ordered by level, cost, card number, name or copies, with Units first, then Pilots, Commands and Bases. The image shows the main deck, its curve and type counts, and a QR code holding the deck list, with steps for importing it.
- On desktop the export dialog shows the **whole deck image** beside its controls, with no scrolling. On phones it fills the screen, so the preview gets the room.
- Import and export plain-text deck lists, and a JSON backup of all decks.
- **Download the latest card database** in one tap: the official card list via gcg-api, with official card images, converted on the device. Check for updates at any time, and a weekly quiet check notes when a newer list is out.
- Install a card catalog from a file or a link, check the link for updates, and go back to the sample at any time. A preview before each install lists any cards your decks use that are missing from the new catalog.
- **App updates**: a daily check while Lock On is open. Windows downloads in the background and installs on close or with **Restart and update**. Android downloads on Wi-Fi and installs with one tap. Each download is verified, and automatic updating can be turned off under Settings.
- Card artwork from catalog links in lists, grids, test hands and card details. Every image downloads once in the background and stays on the device, so cards show their art offline; the Catalog dialog shows how many are saved. On desktop, images are scaled down smoothly, so small card text stays readable in grids. In card details, tap a printing to see its art. It can be turned off in the Catalog dialog.
- On wide windows, the Cards screen keeps a details panel beside the grid. It shows the last card you picked, even after visiting other screens.

**Sync, with no account or server**
- QR pairing with a desktop, with a code check on both screens and a pinned certificate.
- Phones find desktops on the same network and can pair with just the code.
- While both apps are open, changes sync automatically within seconds, and the phone follows a desktop that changes address.
- Edits made on two devices before they sync are kept side by side for you to choose from. Deletions never come back.
- **Send decks**, LocalSend-style: choose decks under Sync, then tap any nearby device to send them, paired or not. It works between a phone and a computer in either direction. The other device asks before saving, and accepted decks arrive as new copies.

**Look and feel**
- Motion throughout:
  - slide and fade-through screen transitions, gliding lists and rolling counts
  - deck gauges, and the **Lock On** moment with a haptic when a deck becomes legal
  - dealt test hands, flipping shields, growing charts, and a reticle spinner while syncing
  - all of it respects the system's "Remove animations" setting
- Rajdhani display type for headings and numbers, and a branded launch screen.
- A bright, game-style look inspired by anime game home screens: frosted glass panels over a sky (or starry twilight) backdrop, rounded Quicksand type, pill buttons and tabs, and floating navigation: a player card with deck and card counts, and a translucent pill nav bar whose selected tab expands with its label. **Settings** gathers sync, the card database and app updates. Hearthstone-style card strips in the deck list, deck boxes in the library, copy pips, deck gauges that turn gold when full, and a level curve beside the deck. Cards lift and lean toward the pointer on hover, flash when a copy is added or removed, and a shine with gold sparkles marks the moment a deck becomes legal. Built on Material 3.
- Phones navigate from a bottom bar; the desktop has a top bar with multi-pane editing.
- A **welcome tour** on first launch offers to download the official card database, then walks through Decks, Cards, Sync and Settings. Skip it at any point, or show it again from **Settings**, which also lists the app's version.

**Desktop**
- Windows installer that installs per user with shortcuts, and needs no administrator prompt.
- Only one copy runs at a time, the window remembers its size and position, and logs go to `%APPDATA%\LockOn\lock-on.log`.

Fresh installs start with a 20-card fictional sample until the real card list is downloaded, from the welcome tour or **Cards → Catalog → Download the latest card database**. Card data comes from gcg-api (ODbL 1.0) and isn't affiliated with Bandai.
