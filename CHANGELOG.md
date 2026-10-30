# Changelog

## 1.4.0 (2026-10-29)

Hear when a sync brings changes, read card details on a phone without scrolling, and a twilight app icon.

- After any sync that moved changes, auto-sync included, a short **snackbar** says what came and went, wherever you are. Syncs with nothing to move stay quiet.
- **Card details on phones** put a smaller card beside its numbers, so the text and the deck controls fit on screen. Tap the card to see it full size.
- **New deck** is a button again: beside the search bar on desktop, and the floating New button on phones, with Import beside the title.
- The **deck search bar** always shows on desktop.
- A **twilight app icon**: the teal Lock On crosshair and red crest on a twilight sky. Android supports themed icons.
- A lighter, more see-through navigation bar, and window controls that sit straight on the sky.
- The player card is now just a bold Lock On name plate, and the start-up screen has a new radar lock-on animation.

## 1.3.0 (2026-10-29)

Send decks by folder, start a deck from the top of your library, and a new app icon.

- **Send by folder**: pick a folder's chip to choose all its decks, and the chooser groups decks by folder. Wide windows show the sending and receiving columns side by side.
- Sends and receives show a **progress bar**, and a gold tick when they're done.
- **New deck** is the first slot in your deck list, on phones too. Tap it to start a blank deck or import a deck list.
- **Export** sits at the top of the deck list in the editor, beside the card/list switch.
- A **new app icon**: a bold navy reticle with a red crest on a yellow tile.
- The start-up screen stays a little longer, so its animation plays through.

## 1.2.0 (2026-10-29)

Look at any card full size, zoom into deck images, and build decks on your phone more easily.

- **Click a card's image** in its details to see it as large as the window allows. It turns toward the pointer, or your finger, and catches the light. Click around it or press Esc to close it.
- **Zoom the full-size deck image**: click, pinch or Ctrl+scroll to zoom, and drag or scroll to move around.
- The **phone deck editor** puts building first, with **Deck**, **Add cards** and **More** sections, a bar to jump between them, and three cards across.
- **Rename** and **Remove folder** are on the folder now: its ⋮ button when you hover it, a right-click, or **More actions** while the folder is open.
- **Choose decks** to send or sync has a search box for deck and folder names. Decks you've ticked stay ticked while hidden.
- Filtering cards fades the results in, instead of sliding cards over each other.

## 1.1.0 (2026-10-29)

Drag decks onto folders, keep empty folders, and name and file a deck as you import it.

- **Drag a deck onto a folder** to move it, with **Undo**. A mouse drags straight away; on phones, hold a deck first so the list still scrolls. Drop it on **No folder** to take it out, or on **New folder** to start one with that deck ticked.
- **Empty folders** stay until you remove them, and **New folder** no longer needs a deck. Removing a folder now asks first and says how many decks it holds; its decks stay in your library.
- Right-click a folder to **Rename** or **Remove** it.
- **Import** lets you name the deck and pick its folder. A deck imported while a folder is open goes into it.
- Menus for sorting, deck actions and the editor are glass pills that tick the current choice, with **Delete** set apart in red.
- The card database is managed from **Settings → Card database** only.
- Calmer deck hangar: deck boxes and cards no longer glow or lean toward the pointer, and scroll bars sit clear of the last column.
- Small cards stay sharp after the large hover preview has shown them, and switches light up under the pointer.

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
- **Deck folders**: a folder list beside the decks on desktop and folder chips on phones. Move decks between folders, create a folder with its decks in one step, and rename or remove folders. Folders sync.
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
- A bright, game-style look inspired by anime game home screens: frosted glass panels over a sky (or starry twilight) backdrop, rounded Quicksand type, pill buttons and tabs, and floating navigation: a player card with deck and card counts, and a translucent pill nav bar whose selected tab expands with its label. **Settings** gathers sync, the card database and app updates. Hearthstone-style card strips in the deck list, deck boxes in the library, copy pips, deck gauges that turn gold when full, and a level curve beside the deck. Cards lift on hover, flash when a copy is added or removed, and a shine with gold sparkles marks the moment a deck becomes legal. Built on Material 3.
- Phones navigate from a bottom bar; the desktop has a top bar with multi-pane editing.
- A **welcome tour** on first launch offers to download the official card database, then walks through Decks, Cards, Sync and Settings. Skip it at any point, or show it again from **Settings**, which also lists the app's version.

**Desktop**
- Windows installer that installs per user with shortcuts, and needs no administrator prompt.
- Only one copy runs at a time, the window remembers its size and position, and logs go to `%APPDATA%\LockOn\lock-on.log`.

Fresh installs start with a 20-card fictional sample until the real card list is downloaded, from the welcome tour or **Settings → Card database → Manage card database**. Card data comes from gcg-api (ODbL 1.0) and isn't affiliated with Bandai.
