# Changelog

## 1.0.0 (unreleased)

First release for Android and Windows.

**Deck building**
- Card browser with search by name, number and text, and filters for color, type, level, cost, set and trait. Sort it, and switch between list and grid.
- Deck editor with one-tap add and remove, and autosave. Cards are tagged **Max** at their copy limit and **Off-color** when they would add a third color, and **Deck colors only** hides the rest. The deck is grouped by card type.
- **Considering** list for cards you're weighing up. It never counts toward the deck.
- Analysis:
  - legality messages
  - Burst and Blocker counts, and Unit–Pilot link pairs
  - level and cost curves, and type and color counts
- **Test hand** using the official setup: draw 5, redraw once, set 6 shields, then draw.
- Deck library with tags, search, duplicate, rename, and delete with **Undo**.
- **Deck folders**: a folder list beside the decks on desktop and folder chips on phones. Move decks between folders, create a folder with its decks in one step, and rename or remove folders. Folders sync.
- Pick any card as a deck's **cover**.
- **Match log** per deck: wins, losses and draws with optional opponent, turn order and notes, and a record with win rate, going first and second, and results by opponent.
- **Version history**: every version of a deck with what changed, a comparison with the deck now, and restoring an older version's cards.
- Export a deck as an **image** (PNG) or a printable **deck registration sheet**, as well as a text list, ordered by level, cost, card number, name or copies, with Units first, then Pilots, Commands and Bases. The image shows the main deck, its curve and type counts, and a QR code holding the deck list, with steps for importing it.
- Import and export plain-text deck lists, and a JSON backup of all decks.
- **Download the latest card database** in one tap: the official card list via gcg-api, with official card images, converted on the device. Check for updates at any time, and a weekly quiet check notes when a newer list is out.
- Install a card catalog from a file or a link, check the link for updates, and go back to the sample at any time. A preview before each install lists any cards your decks use that are missing from the new catalog.
- **App updates**: a daily check while Lock On is open. Windows downloads in the background and installs on close or with **Restart and update**. Android downloads on Wi-Fi and installs with one tap. Each download is verified, and automatic updating can be turned off under Settings.
- Card artwork from catalog links in lists, grids, test hands and card details, cached per device. In card details, tap a printing to see its art. It can be turned off in the Catalog dialog.

**Sync, with no account or server**
- QR pairing with a desktop, with a code check on both screens and a pinned certificate.
- Phones find desktops on the same network and can pair with just the code.
- While both apps are open, changes sync automatically within seconds, and the phone follows a desktop that changes address.
- Edits made on two devices before they sync are kept side by side for you to choose from. Deletions never come back.

**Look and feel**
- Motion throughout:
  - slide and fade-through screen transitions, gliding lists and rolling counts
  - deck gauges, and the **Lock On** moment with a haptic when a deck becomes legal
  - dealt test hands, flipping shields, growing charts, and a reticle spinner while syncing
  - all of it respects the system's "Remove animations" setting
- Rajdhani display type for headings and numbers, and a branded launch screen.
- A bright, game-style look inspired by anime game home screens: frosted glass panels over a sky (or starry twilight) backdrop, rounded Quicksand type, pill buttons and tabs, and floating navigation: a player card with deck and card counts, and a translucent pill nav bar whose selected tab expands with its label. **Settings** gathers sync, the card database and app updates. Hearthstone-style card strips in the deck list, deck boxes in the library, copy pips, deck gauges that turn gold when full, and a level curve beside the deck. Cards lift and lean toward the pointer on hover, flash when a copy is added or removed, and a shine with gold sparkles marks the moment a deck becomes legal. Built on Material 3.
- Phones navigate from a bottom bar; the desktop has a top bar with multi-pane editing.

**Desktop**
- Windows installer that installs per user with shortcuts, and needs no administrator prompt.
- Only one copy runs at a time, the window remembers its size and position, and logs go to `%APPDATA%\LockOn\lock-on.log`.

Fresh installs start with a 20-card fictional sample; **Cards → Catalog → Download the latest card database** brings in the real card list. Card data comes from gcg-api (ODbL 1.0) and isn't affiliated with Bandai.
