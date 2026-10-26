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
- Import and export plain-text deck lists, and a JSON backup of all decks.
- Install a card catalog from a file or a link, check the link for updates, and go back to the sample at any time. A preview before each install lists any cards your decks use that are missing from the new catalog.
- Card artwork from catalog links in lists, grids, test hands and card details, cached per device. In card details, tap a printing to see its art. It can be turned off in the Catalog dialog.

**Sync, with no account or server**
- QR pairing with a desktop, with a code check on both screens and a pinned certificate.
- Phones find desktops on the same network and can pair with just the code.
- While both apps are open, changes sync automatically within seconds, and the phone follows a desktop that changes address.
- Edits made on two devices before they sync are kept side by side for you to choose from. Deletions never come back.

**Look and feel**
- Material 3, with a mobile-suit color scheme in dark and light themes, the Lock On V-fin mark, and armor-plate panels.
- Phones navigate from a bottom bar; the desktop has a top bar with multi-pane editing.

**Desktop**
- Windows installer that installs per user with shortcuts, and needs no administrator prompt.
- Only one copy runs at a time, the window remembers its size and position, and logs go to `%APPDATA%\LockOn\lock-on.log`.

The built-in card catalog is a 20-card fictional sample. Install a real one from **Cards → Catalog** once you have a data source you're allowed to use (see docs/CATALOG_FORMAT.md).
