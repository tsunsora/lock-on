# Changelog

## 26.11.25 (2026-11-12)

On the computer, moving around is smoother: opening and leaving a deck and changing the theme drop far fewer frames, and the theme no longer jumps as it changes.

- **Opening and leaving a deck:** the slide used to stutter the whole way across. The back button growing into its corner made every screen measure itself again on each frame, and the slide itself was more work than it looked. Both are fixed, so the deck glides in and out.
- **Switching screens:** the screen you are going to is made ready in the same moment as your click, instead of part way through the old screen's fade, where it showed as a hitch. Deck editors kept ready in the background no longer rebuild themselves each time you switch tabs or open another deck.
- **Changing theme:** the fade from Sky to Twilight and back played at about a third of its frames, because every button and tab was also changing color underneath it. Now the old look holds for a moment while the new one is made ready, then fades smoothly.
- **Theme jump:** on Windows, the screen shifted up by the height of the title bar while the theme changed, then dropped back. It stays put.

## 26.11.24 (2026-11-10)

On the computer, the bar across the top of the app is slimmer, now that the window's buttons have a title bar of their own.

- **Top bar:** it is about a quarter shorter. The Decks, Cards, Sync and Settings pill is the height of the buttons below it, and its edges line up with the panels underneath.
- **Lock On's name:** on Windows it now shows once, in the title bar, instead of again just under it. Inside a deck the top-left corner holds the back button, with a little more room before the deck's name.
- **Narrow windows:** a deck's name and state, when they stack on two lines, no longer crowd the top of the window.

## 26.11.23 (2026-11-10)

On Windows, Lock On has a title bar of its own, like other Windows apps.

- **Title bar:** minimize, maximize and close moved out of the app into a strip of their own above it, with Lock On's mark and name. The buttons are the size of Windows' own, and close turns red when you point at it. Drag the strip to move the window, or double-click it to maximize. It follows the light or dark theme.
- **Starting up:** the startup animation plays under the title bar instead of covering it.

## 26.11.22 (2026-11-10)

A visual tidy-up: a quieter sky, lighter type, even spacing and one set of icons, so the cards carry the color.

- **Sky and panels:** the sky is one quiet gradient with no glows or beam of light. Panels have one thin edge instead of a rim that fades from bright to dim, and shadows are neutral instead of blue. A card's details no longer glow in the card's color, and a transfer's progress bar and the welcome tour's picture band are flat.
- **Type:** headings, labels, buttons and numbers are a little lighter, so size and color set them apart rather than boldness. The launch screen's readouts are plain words instead of spaced capitals.
- **Spacing and corners:** gaps between things sit on one even grid, deck tiles have even padding, and a rounded shape inside another follows the outer one's curve. Menus and notices share one corner size.
- **Icons:** every icon is an outline from the same set; the few filled arrows are redrawn to match. Analysis tiles show a label and a number, with no icon.
- **Wording:** one-line notices end without a period, the welcome tour opens on Lock On's name, and the line under Delete says you can undo it right after.
- **Motion:** switches and a card held up to inspect settle without bouncing, and a hovered card lifts without growing.

## 26.11.21 (2026-11-10)

The desktop app rests when nothing is moving, and cards scroll more smoothly.

- **At rest:** a spinner on the Sync page kept the whole window redrawing all the time, on every screen, even with nothing moving. The window now draws only when something changes, so Lock On asks far less of the PC while it sits open.
- **Scrolling cards:** each row of cards is made ready just before it scrolls into view instead of as it appears, so scrolling Cards, a deck's card list and the deck library drops far fewer frames.
- **Saving decks:** a deck saves itself a moment after every change. That save no longer rebuilds every open screen, so adding cards one after another stays smooth.

## 26.11.20 (2026-11-08)

Decks from the popular Gundam deck sites paste straight in, and the desktop app no longer freezes every 30 seconds.

- **Importing decks:** paste a deck from ExBurst, Limitless, DeckPlanet, Egman or whitebase.dev into New deck, as a deck link, a list or a JSON export, and it comes in as a deck.
- **No more 30-second freeze:** the desktop app used to pause for a moment every 30 seconds while anything was moving, often right as you clicked. That pause is gone.
- **Opening decks:** the first deck you open and your first visit to Cards no longer stutter in a big window, and reopening a deck stays smooth.

## 26.11.19 (2026-11-08)

The desktop app no longer stalls every 15 seconds, and dragging the deck editor's divider is smoother.

- **Fewer stalls:** checking the PC's network for phone sync used to freeze the window briefly every 15 seconds, whatever you were doing. It now happens in the background.
- **Divider:** dragging the line between the card list and the deck lays the editor out once a frame instead of on every mouse movement, so big windows keep up with the pointer.
- **Scrolling:** cards coming into view set up a little less work.

## 26.11.18 (2026-11-07)

Logged games can be edited, the Card database is easier to read, and adding cards uses the deck's own + and −.

- **Editing games:** tap a logged game to change its result, opponent, turn order or note. Games save the moment they're logged, and a quick second tap can't undo the first.
- **Matches page:** a just-logged or edited game scrolls into view and lights up, and the tapped result reads Logged for a moment. Games come before By opponent, games with no turn order get their own row, and the phone's record takes less room.
- **Card database:** the installed list's facts sit on an even grid with its status first, an update the weekly check found is one tap, and other sources open in a panel of their own. The licence credit stays in view.
- **Adding cards:** the + and − under each card in Add cards now match the deck view, on phones and desktop: a teal count with copy pips, and the buttons at the card's edges. The list view uses the deck list's buttons too.

## 26.11.17 (2026-11-07)

Syncing recovers on its own when one side forgot the other, and the Sync page has more room.

- **Pairing again:** when a desktop has forgotten a phone that still remembers it, the phone now answers the desktop's invitation instead of leaving it waiting. Tapping Pair during a sync waits for that sync, or says why it can't, instead of doing nothing.
- **Sync page on desktop:** this device, your devices and nearby devices each sit on a solid panel with more space between them.
- **Sync page on phones:** the devices sit above a tray with this device, its pairing options and the tabs, all within thumb reach, and status lines stay on one line.
- **Progress:** busy rows show a plain progress track instead of a light running round the edge, and finished sends get a gold tick.

## 26.11.16 (2026-11-07)

A launch screen that matches the new logo, and a permanent signing key for the Android app.

- **Launch screen:** the sky leans violet and the disc is now the real logo, in day and twilight, with gold kept for the moment it locks on. It fades into the app more gently, and Android's splash screen matches.
- **Android:** updates are now signed with Lock On's own key. Updating works as before, with nothing to uninstall.

## 26.11.15 (2026-11-06)

A new logo, cleaner colors, and calmer touch feedback: nothing glows or sweeps across a card when you press it anymore.

- **Logo and icon:** a bolder Lock On mark that stays clear at small sizes, on a violet tile. The app icon, phone icon and splash screen match.
- **Pressing things:** every tile, card, row and icon button now answers with one flat, quick shade instead of a spreading ripple.
- **Adding and removing cards:** a brief solid outline shows which card changed, teal when adding and red when removing, instead of a colored flash.
- **Hover and inspecting (desktop):** cards rise with an outline and no colored shadow. An inspected card still tilts, but without the moving light, and Sync no longer has a light following the pointer.
- **Phone buttons and holds:** main buttons lose their teal shadow, and holding the export preview shades it instead of growing a glow.
- **Colors:** the day teal is a little deeper so white text on it is easier to read, the app uses one gold throughout, and twilight's selected switches are easier to read.
- **Deck tiles without art (phones):** these now use the same calm flat color as desktop.

## 26.11.14 (2026-11-06)

Deck tiles show more of what matters in their art: phones now zoom in on the illustration, and desktop shows a little more of it.

- **Deck tiles (phones):** the cover card is zoomed in on its illustration instead of showing the whole card, so the cost, name and rules text no longer appear on the tile.
- **Deck tiles (desktop):** the cover card is zoomed out slightly, so more of the illustration shows.

## 26.11.13 (2026-11-05)

The back button's rounded corners no longer show a blue tint.

- **Back button (desktop):** the logo's color no longer shows through at the button's corners inside a deck.

## 26.11.12 (2026-11-05)

Desktop deck tiles now match the phone, with the card at the side, so far more decks fit on screen.

- **Deck tiles (desktop):** each deck shows its cover card at the side, zoomed in on the art, next to its name and colors, its state, record, win rate and age, and its gauge.

## 26.11.11 (2026-11-05)

Desktop deck tiles give their art more room.

- **Deck tiles (desktop):** the art is taller, and the main deck gauge is now a thin line along its lower edge. Below it are the deck's name and one line for its colors, state, record and age.
- **Deck tiles (desktop):** an unfinished deck's tag shows its count, such as **34/50**.

## 26.11.10 (2026-11-05)

Desktop deck tiles now lead with big, bright card art, and right-clicking text opens a menu that matches the app.

- **Deck tiles (desktop):** each deck shows one large crop of its cover card's art in place of the dimmed band and small cards. Colors, state, record and age share one line, and the win rate is now only under **Matches**.
- **Deck tiles (desktop):** a deck past the limit says how far over it is in red, such as **3 over**. A deck with a conflict says **Review**. Decks without art show their colors and a shield.
- **Right-click (desktop):** text opens a menu on the app's own panel with Undo, Redo, Cut, Copy, Paste and Select all, their shortcuts, and only what applies.
- **Deck editor (desktop):** the deck's first problem sits in the header beside its state, so it coming or going never moves your cards.
- **Export:** **Save PNG** goes straight to Downloads, next to any file of the same name. The save dialog opens only when that can't be done, as on Android 8 and 9.
- **Phones:** dialogs shrink to fit above the keyboard, so Cancel and Create stay in reach, and the New deck dialog scrolls when there isn't room for all of it.

## 26.11.9 (2026-11-05)

Everything that used to switch at once now moves, and desktop controls share one look.

- **Motion:** dialogs fade out as they close, and the dim behind them eases in and out. On desktop, changing the theme fades across.
- **Motion:** outlines, the + and − buttons, copy dots, card tags and buttons ease between states. The card preview and dragged cards and decks fade away instead of vanishing.
- **Motion:** Sync, Analysis, Matches, Versions, Export, the update card and the welcome tour fade between states, and counts and bars move to their new values. Test hands make room for a drawn card.
- **Desktop controls:** buttons, chips and steppers share two heights and one flat fill, one step deeper on hover and again when pressed.
- **Desktop selection:** selected and hovered things get one solid teal line right on their edge, with no gap or gradient. Deck art is a little darker.
- **Deck colors:** pressing a color under **Deck colors** turns it off and toggles that color, keeping the deck's others picked. Turning it back on clears the picks.
- **Buttons:** they always come back up after a click, and the sort button no longer opens a gap around itself when pressed.
- **Deck editor:** the divider loses its glow. Hovered or dragged, its grip only turns the accent color.
- **Phone Matches:** the logging panel rises above the keyboard, so the Opponent and Note fields stay visible while you type.

## 26.11.8 (2026-11-05)

Phone screens get a cleaner, more solid layout, the background is quieter so cards stand out, and the boot screen plays smoothly on phones.

- **Phones:** Filters and card details are floating sheets with a button that closes them. **Show N cards** closes Filters, and **Clear** resets it.
- **Phones:** Analysis, Test, Matches, Versions, Settings and Sync sit on solid panels, with their main controls just above the bottom bar. The More menu dims the screen and lights its button.
- **Phones:** the boot screen no longer stutters while Lock On starts. It holds its first frame until it's ready, then plays through without pausing while the app builds behind it.
- **Phones:** Lock On gets quicker to start after the first few launches, once Android has prepared it in the background.
- **Background:** the sky is quieter in both themes, a deep navy at twilight and a pale grey-blue by day, so cards stand out.
- **Blur:** gone everywhere, including behind dialogs.
- **Card details on desktop:** shorter and a little narrower, with the type beside the card number, smaller stat boxes, and traits, zones and Link on one line. Deck and Side deck counts share a row.
- **Deck builder:** the card list has the same grid and list switch as Cards. The deck's color dots at the top are larger, matching the Legal badge.
- **Decks:** a deck's art sits back as a soft, darker wash, so its cards and color dots stand out. The fan is a little bigger, and the taller banner no longer cuts off its middle card. **New deck** sits right after the sort button.
- **Cards:** color letters and cost numbers sit in the middle of their dots, and the number and copy dots under a card sit in the middle of their plate. Cards without artwork center their icons, and small labels sit centered.
- **Dialogs:** a long card or deck name stops short of the **Close** button.
- **Navigation:** inside a deck, the back button and logo plate line up with the middle of the navigation pill.
- **Settings:** switch rows lose their square hover highlight. The switch brightens instead, and the whole row still toggles it.

## 26.11.7 (2026-11-05)

The rest of the new look: the screens that still followed the old layout now match the design.

- **Decks:** each tile shows three of the deck's cards over its main card's art, with when it was last edited. Folders sit in a panel with icons, and there's a sort (Last edited, Name, Win rate). Ctrl+F searches decks on this screen.
- **New deck:** one dialog for everything: name, folder, and start empty or paste a list, which counts the cards as you type. It takes files and Lock On backups too.
- **Welcome tour:** one picture and one line per stop.
- **Test:** the hand is held in a fan, the six shields are always there, and Redraw, Draw and New game sit in one row. The first draw keeps the hand.
- **Analysis:** tiles for Burst, Blockers and Links, Level and Cost charts, and bars split by type and color.
- **Matches:** a win-rate ring, bars for going first, going second and each opponent, and shorter game rows. Tap a game to see its time or remove it.
- **Versions:** Now on top, and older versions open in place to show what restoring would change.
- **Deck editor:** the back button is plain, the header is one line, and rows are − count +. Settings can open decks as card images by default.
- **Sync:** this device has QR code and Address beside it, and every row has its action. Send decks keeps your choice in a tray above the same lists.
- **Settings and Cards:** one-line rows with shorter names, and the card details put the card beside its numbers.
- **Icons and details:** outline icons everywhere, a smaller logo inside its plate, flat gauges and switches, softer chips, and the app blurs behind dialogs where the device can.
- **Compare and pairing:** versions to choose between show their device, gauge and each card's difference side by side. Pairing shows the number as large digits.
- **Card database:** the installed list as a card with its version, count, source and date, with other sources always in view.
- **Phones:** deck rows with the card on a strip of its art, folders as tabs, Deck, Add and More always labelled, the card details as a sheet, and a first-run screen with three ways to get a deck.
- **Card search:** search and deck list import find names as they can be typed, such as Üso Ewin by "uso".
- **Phones:** the deck's first problem sits in the editor's header, and card details fill the screen more cleanly.

## 26.11.6 (2026-11-04)

A new look, after Nintendo's recent menus: calm solid panels, rounded rectangles and a clearer selection.

- **Everywhere:** panels are solid instead of see-through, and buttons, chips, fields and tabs are rounded rectangles. Solid teal now marks navigation and main actions; other choices use a white thumb, and filters a teal outline. Charts are one slate colour.
- **Logo and icon:** a new rounded tile in teal and blue, with a white reticle by day and bright cyan with a navy reticle at twilight. The app icon matches.
- **Cards without artwork:** a simple flat face with the card's colour, one type symbol, its cost and level, a one-line name and AP / HP. Face-down cards are blue to match.
- **Decks:** smaller tiles, four or more across on a computer, with the cover card on a band in the deck's colours. A deck that needs review no longer shows a gold gauge. On phones, New deck is a round + at the bottom right.
- **Deck editor:** the gauge, Export and More now sit at the top of the deck panel, and its tabs have icons. Decks open as rows, each with its cost, a small picture and its name; Card view is under More. The Deck and Side deck switch no longer needs an "Add to" label, and the header is just the name, colours and state.
- **Navigation:** Decks and Cards have card-shaped icons.
- **Settings and Sync:** Settings is two centred columns with no side rail. Sync is one centred column, without the explanatory paragraph.
- **Matches:** Win, Loss and Draw are three filled buttons.
- **Dialogs:** they close with a Close or Cancel button instead of a corner ×.
- **Pointer:** a teal-to-blue ring follows the card under the pointer. Screen titles beside the navigation are gone, since the lit item names the screen. Twilight is a softer dusk.

## 26.11.5 (2026-11-03)

The boot screen stays in one piece at any window size.

- **Startup:** the boot screen no longer pulls apart when the window opens maximised or changes size as Lock On starts. The mark stays centred with the radar around it, and the whole window stays covered.
- **Computer:** the boot screen plays at a steady 60 frames a second in a maximised window, up from about 35.
- **Phones:** turning the phone while Lock On starts keeps the mark centred in the radar.

## 26.11.4 (2026-11-03)

The boot screen plays evenly from start to finish, and Lock On feels smoother throughout.

- **Startup:** the boot screen no longer freezes or speeds up for a moment as it starts. On Windows it opens on its first frame and moves evenly from then on, until it fades into the app.
- **Speed:** adding a card, saving a deck and searching do less work, and Sync no longer keeps the window redrawing while it's out of sight.
- **Motion:** dialogs and menus fade in as they open. Editor headers, banners and notes glide into place, the deck gauges warm to gold and red, and the level curve grows in.
- **Motion:** the test hand, card views, printings, the Settings rail and the welcome tour move between states instead of switching. On phones the floating bar hands over smoothly to the deck editor's.

## 26.11.3 (2026-11-03)

Lock On no longer sometimes hangs when it starts on Windows, and the Cards screen has new sorts and filters.

- **Computer:** fixes a hang in 26.11.2 where Lock On could stay on the boot screen for good when it started.
- **Startup:** the boot screen hunts all the way in, locks on and holds the lock until it fades, rather than letting go and hunting again.
- **Cards:** sort by **Newest** (latest set first), **AP** or **HP**, and pick in **Settings** which order the Cards screen opens in. A new sort starts at the top of the list.
- **Cards:** **Filters** puts Level, Cost, AP and HP on one strip of numbers each, to tap or drag across. **Set** is a menu beside Trait, and **Clear** resets them all. A button takes you back to the top once you've scrolled down.
- **Export:** the shared deck image's header shows the name, colors, count and legality, and how many Units, Pilots, Commands and Bases the deck has. Each card's count sits just under its number and rarity.

## 26.11.2 (2026-11-03)

The boot screen plays smoothly while Lock On starts, and the shared deck image is shorter.

- **Startup:** the boot screen no longer freezes while the app loads behind it, then fades into the app. On phones it replaces the old launch screen, matching light or dark.
- **Export:** the shared deck image says each thing once. The header sits on the cover card's art with the name, colors, count and legality, beside the QR code, and each card's count sits on its top-right corner.
- **Phones:** a deck always opens on its deck list. Only a deck you've just made opens on **Add cards**.
- **Android:** switching to another app and back no longer brings the keyboard up again. Your search text stays.
- **Toasts:** the message sits centred in the pill, without the dot in front.

## 26.11.1 (2026-11-02)

Versions are now numbered by date, and Sync shows this device on its own panel.

- **Versions:** numbers are now year, month and release, so this is the first release of November 2026. Updates from 1.13.4 arrive as usual.
- **Sync:** this device has its own panel with how the last sync went, beside the device list on wide windows and above it on phones. Device tiles light up under the pointer and while they sync or send.
- **Computer:** inside a deck, the Lock On plate in the corner turns into the way back to **Decks** (or back from **Compare versions**), in place of the editor's own back button.
- **Export:** the full-size image grows out of its preview and shrinks back into it.
- **Phones:** the full-size image fills the screen and zooms by pinch or double tap. Export has a third format, a large QR code of the deck, with **Copy text** and **Save QR**.

## 1.13.4 (2026-11-02)

The divider in the deck editor stays where you put it, and Settings has a new layout.

- **Computer:** the divider between the card list and the deck is the same for every deck and stays put after a relaunch. **Settings › Decks** can reset it.
- **Computer:** the full-size deck image opens fitted to the window, and you can still zoom in to read the cards.
- **Phones:** the deck editor's **More** menu opens as a grid of tiles above the bar, and **Settings › Deck editor** can leave sections out of it.
- **Phones:** Export shows the deck list as rows with counts and card numbers, switches between List and Image, and can open the image full size.
- **Settings:** each setting is a row with its name and control, and wide windows get a rail to jump between sections.

## 1.13.3 (2026-11-02)

Opening a deck starts at the top of its card list and deck list.

- **Deck editor:** a deck you open again no longer comes up halfway down where you last left it. Your search and filters stay as they were.

## 1.13.2 (2026-11-02)

Once a deck has all its colors, adding cards shows only the ones that fit.

- **Deck editor:** **Deck colors only** turns on by itself the first time the main deck reaches its color limit. Turn it off and it stays off.
- **Settings:** a switch under **Decks** stops it turning on by itself. Sync copies it to your other devices.

## 1.13.1 (2026-10-31)

Making the window bigger no longer flashes white and black.

- **Computer:** while you drag the window larger, the new edge fills with the app's background instead of white and black strips.
- **Phones:** the deck editor's **More** menu shows an icon beside each page, with new icons for **Matches** and **Versions**.

## 1.13.0 (2026-10-31)

Syncing now makes one device match the other, with everything on it.

- **Sync:** the Sync tab opens on **Sync devices**. Tap a paired device and choose **Make this phone match it** or **Make it match this phone**. The device being replaced takes on the other's decks with their history, folders (empty ones too), match logs, settings, chosen ruleset and card list. Decks only on that device are removed, and earlier versions of replaced decks stay in their history. Lock On asks before replacing anything.
- **Merge:** **Merge both ways** is still there, beside matching, and keeps every deck from both devices. On a phone, **Keep merging** is now set per computer. Computers you'd already paired keep merging; a newly paired one doesn't until you turn it on, so a new phone can be matched first.
- **Computer:** a sync started on the computer is carried out by the phone, so Lock On needs to be open on it. The phone asks before its own decks are replaced.
- **Send decks:** sending a few decks to anyone nearby moves to its own page under Sync.
- Both devices need this version to make one match the other.

## 1.12.0 (2026-10-31)

Buttons answer every tap, and backing up your decks moves to Settings.

- **Everywhere:** buttons, chips and switches visibly press even on a quick tap, and tint on hover. Dialogs grow in as they open.
- **Settings:** **Decks → Back up all decks** saves every deck to one file. It's no longer in the Decks screen's menu.
- **Computer:** **Edit as text** is next to Export in the deck editor and opens in a dialog. The deck editor bar setting only shows on phones.
- **Export:** hold the image preview briefly to see it full size; a tap no longer opens it, so the phone preview scrolls freely.

## 1.11.0 (2026-10-31)

On phones, the deck editor's bar can be arranged the way you like, and the deck list can be edited as text.

- **Phones:** **Settings → Deck editor bar** puts More, Add cards and Deck in any order, and pages slide in that order.
- **Phones:** Export and the new **Edit as text** open as pages of the deck editor, under More. Edit the list and apply it to the deck; lines it can't read are named first, and cards keep their art.
- **Phones:** hold **Deck** to switch the deck list between cards and a list.
- **Cards:** hold **Filters** to clear every filter and the sort, keeping what you typed.

## 1.10.0 (2026-10-31)

New sets' alt arts arrive sooner, and the deck editor is easier to use on phones.

- **Card database:** alt arts that are already on Bandai's official card list but not yet in the weekly card database come straight from Bandai's site, so GD06's parallels are here now. **Check for updates** offers them even when the card database itself hasn't changed.
- **Phones:** the deck editor's bar reads More, Add cards, Deck, and swiping along it switches between your deck and the card list. Export and the card/list switch are under More.
- **Export:** the text list can be edited and applied back to the deck.
- **Folders:** New folder just asks for a name. A deck dropped on New folder still goes into it, and a new deck scrolls into view in the library.

## 1.9.5 (2026-10-30)

Card pictures load faster on Windows.

- **Windows:** card pictures come up about a third faster, so scrolling through cards stutters less while new ones load.

## 1.9.4 (2026-10-30)

Lock On opens faster on Windows.

- **Windows:** Lock On starts over a second sooner, and the launch screen keeps moving while it loads instead of freezing.
- **Windows:** opening maximised no longer shows a white patch while the window grows.
- **Windows:** updates never add a desktop shortcut. One already on your desktop stays. This takes effect from the update after this one, since the old version runs this update's installer.

## 1.9.3 (2026-10-30)

The desktop window remembers being maximised.

- **Windows:** close Lock On maximised and it opens maximised next time.
- **Windows:** updates no longer bring back a desktop shortcut you deleted, or add a second one beside one you renamed. This takes effect from the update after this one, since the old version runs this update's installer.

## 1.9.2 (2026-10-30)

Decks drop their tags.

- **Deck tiles** in the library no longer show tags, and a deck's menu no longer has **Edit tags**. Folders and search keep a collection tidy.
- Tags already saved on a deck still appear in its text export and travel with it when shared.

## 1.9.1 (2026-10-30)

Base tokens stay out of decks.

- **Axis** (T-037), a base token, no longer shows in the deck editor's card list. The official card list files it as a plain Base, so it now gets its own **Base token** type, which the Cards screen shows as a type chip.
- If your installed card database has Axis as a Base, **Settings** offers the card database again. Installing it moves Axis to Base token.

## 1.9.0 (2026-10-30)

A roomier phone deck editor.

- On phones, the **Add to** switch moves into the editor's header. While you add cards, a Side deck chip with its count sits beside the Main gauge, and whichever one the card list's + fills is the bright one. The card list gets back the row the switch used to take.
- On phones, the deck editor's top bar has **Export** again, as on desktop.
- On phones, the editor's bottom bar puts **Add cards** before Deck. Tapping the open Add cards or Deck tab again scrolls its list back to the top.

## 1.8.1 (2026-10-30)

A brighter app icon.

- The **app icon**'s tile is a brighter navy with a diagonal gradient, lighter at the top right and darker at the bottom left, on Windows, macOS, the window icon and the Android launcher. The splash screen keeps a solid navy.
- The in-app Lock On badge goes back to the page background.

## 1.8.0 (2026-10-30)

Tokens in the card list, simpler folder controls on phones, and a tidier card dialog.

- The **Cards** tab lists tokens (Unit tokens, EX Bases and EX Resources) under their own types, so you can find and filter them. The deck editor doesn't offer them, a deck holding one is flagged, and a pasted list's card name never means a token. An official list converted without its tokens is offered again.
- On phones the **folder chip**'s menu only picks a folder: New folder is a round + button before the chip, and the open folder's Rename and Remove are round buttons after it.
- The **deck library** drops its tag filter chips. Decks still show and edit their tags.
- On desktop the **card details** dialog lists printings five to a row wherever five fit, and narrower columns share them evenly. On phones the dialog is more compact, with the type in the id line, traits as a line of text, and smaller zone chips, stats, card text and printing tiles.
- The Lock On badge sits on a lighter tone.

## 1.7.0 (2026-10-30)

A new app icon, roomier card details on desktop, and lighter toasts.

- A new **app icon**: a white crosshair with a center dot on a navy tile, on Windows, macOS and Android. The in-app Lock On mark uses the same center dot.
- On desktop, the **card details** dialog is wider and lists printings beside the card text, in one row when they fit, instead of stacking them under the art.
- **Toasts** are glass pills sized to their text, with the action as a ghost button.
- The wide deck screen's header drops its Export button; Export stays in the deck panel's bar.

## 1.6.0 (2026-10-30)

A test hand that fits on one screen, and folders and filters that fit on a phone.

- The **test hand** shows its state as readouts, keeps all six shields in one row under the hand, and sizes cards so the hand and shields fit without scrolling. Cards drawn later are tagged, and Keep, Redraw and Draw are wide buttons.
- On phones, **folders** are one chip naming the open folder. Its menu lists every folder with its count, New folder, and Rename and Remove. While you hold a deck, the folders spread out as chips to drop it on.
- On phones, the card list's colour orbs and type chips fold into **Colors** and **Type** chips, and the Filters drawer scrolls, so every filter is reachable on a short screen.
- The card details' **In this deck** panel takes less height on phones: the deck and side deck steppers share a row.
- The **export dialog** shows its own messages when you copy or save the list or image.
- Phones export from the deck library's menu instead of a button above the deck list, and the wide editor opens its deck panel on the start tab.
- The Lock On badge is the same teal-lit glass as its name plate.

## 1.5.0 (2026-10-29)

Clearer printings, a roomier deck screen on phones, and importing from New deck.

- **Printings** in card details are glass tiles with the shown one lit and checked. On phones they scroll sideways, and on a deck's card the tiles pick the printing the deck uses.
- **New deck** offers *Import a deck list instead* under the name field, in place of the separate Import buttons.
- On phones the **deck screen's cover art** reaches up under the status bar, and the − and + under each card sit evenly from either edge.
- The phone deck bar's **Export** and **List view / Card view** switch match desktop, and the share icon leaves the header.
- The deck header's cover art is fainter on desktop so the navigation reads clearly, and the phone's floating navigation bar is nearly solid.

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
