# Lock On UX rules

Every screen and change follows these rules. If a change breaks one, either fix the change or change the rule here first, with the reason.

Two craft guides set the floor for how things look and behave: [unslop](https://github.com/heydamir/unslop) (spacing, colour, icons, copy, type, details and motion, in that order) and [impeccable](https://github.com/pbakaus/impeccable) (product UI, Android, states, motion and critique). Lock On follows both except where its own direction says otherwise, and each of those choices is written down with its reason in [Where the guides disagree](#where-the-guides-disagree). Places where the app doesn't meet a rule yet are listed in [Known gaps](#known-gaps), so a rule is never quietly broken.

Sources and what we took from each are in [UX research](#research-behind-the-rules) at the end.

## 0. Direction

Lock On is a working tool first. In impeccable's terms every screen is an **Operate** surface: a player came to build or test a deck, so familiar controls, scanning and consistency come before expression, and the identity lives in type, palette, density and one signature. In unslop's terms the direction is:

- **Product.** A deck builder for the Gundam Card Game, on phone and desktop, that syncs between them without an account.
- **Character.** Calm, quick, a little playful.
- **World.** A dusk sky over a hangar, after Stella Sora's home screen and Nintendo's recent menus: night ink, teal instrument light, one gold for the lock-on.
- **Moments.** The cards, which are the one bright thing on any screen, and the Lock On moment when a deck becomes legal (V5).
- **Signature.** The Lock On reticle. It is the mark, the progress spinner, and the brackets that close when a deck becomes legal. Nothing else borrows its shape.

**Template check.** Cover the name: if a screen could belong to any other app, push the direction harder in its copy, its cards and its signature, not by adding decoration.

## 1. Navigation and structure

- **N1. Four destinations, no more.** The top level is **Decks**, **Cards**, **Sync** and **Settings**. Sync holds pairing and devices; Settings holds the device's own preferences, the card database and app updates. A new feature goes inside an existing destination unless it is used as often as those four. (Aniimo opens on a 16-icon menu and reviewers call it a nightmare to navigate. Both guides keep top-level navigation to five items or fewer.)
- **N2. Two levels deep at most.** A destination's own screen counts as level one. A deck's editor or a conflict comparison counts as level two. Anything further opens as a sheet or dialog over level two, never a third screen. (Stella Sora is praised for having few nested menus.)
- **N3. Back always works and never loses work.** System back (including Android's predictive back gesture), the on-screen back arrow and Escape on desktop all leave the current screen, and edits are already saved when they do. Nothing traps or hijacks the gesture.
- **N4. Wide windows split into panes instead of adding screens.** At 840 dp and wider, the editor has two areas: the card list and the deck. On phones the same views become tabs.
  - Width follows use. The card list gets about 60%. The deck panel gets the rest, and its tabs (**Deck**, **Analysis**, **Test**, **Matches**, **Versions**) hold everything about the deck, so nothing else takes width from building.
  - The deck panel opens with the gauge, Export and More above its tabs. The Deck tab starts straight on the list, under the first legality problem in words, which links to Analysis. The curve and counts live in Analysis.
  - The Cards tab shows details in a side panel instead of a dialog.
  - The library lays decks out in a grid.
  - Reading screens such as Sync stay under about 980 dp wide.
  - Phones show three cards across.
  - **Space goes to cards.** The card list header is two rows: the search with its result count, Filters, Sort and the view toggle (and, in the editor when the list is at least 720 dp wide, the **Deck · Side deck** switch), then colours as orb toggles with initials and types as chips. The second row wraps on wide panes. On a phone's Cards screen it stays one row that scrolls sideways, fading at its edge to show there is more; in a phone's deck that row holds Deck · Side and Deck colors, with colours and types in Filters. Every row that scrolls sideways fades at its edge the same way (`fadeEnd`), never cutting a chip off hard. On phones, Filters is a floating sheet over a plain dim: sort and Grid/List, then each stat as one strip, then Set and Trait, with **Clear** and a teal **Show N cards** that closes it. Results follow every tap behind it, so there is no Apply. Its chip lights and counts the filters set in the sheet, and holding it resets them. On wide panes Filters unfolds in place, and **Clear** does the same reset.
  - **Sheets and panels on phones.** Filters and card details share one floating solid panel, closed by a button, by tapping the dim or by Back. Text, settings and controls that sit on the sky go on solid panels (`SoftTile(solid = true)`); only cards lie on the sky itself (V3). A page's main controls (the Test buttons, match logging, Keep as a version) stay above the bottom bar.
  - Deck strips are 48 dp tall on wide windows and 56 dp on phones. The level curve above the deck list is a 48 dp row with a small caption.
  - On phones, a screen's title shares its row with that screen's main action. Rarely used actions go in a ⋮ menu, or in Settings when they cover the whole app, like Back up all decks. Folders and tags share one row: phones pick a folder from one chip naming the open folder, whose menu lists every folder with its count, New folder, and Rename and Remove for the open one. While a deck is held, the folders spread out as chips so it can be dropped on one.
  - The test hand is held in a fan, like a hand at the table: each card overlaps the one before and tilts a little, and past ten cards a new fan starts under it. Cards are about a third of the width (two fifths on a phone) and never narrower than keeps their names, and they are sized so the opening fan and the shields fit without scrolling, above Keep and Redraw. Its six shields share one row under the hand. Cards drawn later carry a *Draw n* tag in the corner that stays in view.
- **N5. Floating navigation shares the top band.** Navigation floats over the sky as detached plates instead of spanning the window (V9).
  - At 840 dp and wider, the brand, or inside a deck the back button, floats top-left and the navigation pill top-right, 12 dp from the window's sides and from the top, in line with the panels below. Each screen puts its own header in the band between them (`ScreenHeader`, or `clearOfNav` for custom headers such as the editor's).
  - **On desktop the band is 64 dp.** The pill and the back button are `ControlSize`, 40 dp, the height of the toolbar buttons under them (K1), with 18 dp icons. Wide touch screens keep the phone's 60 dp pill. (The band was 84 dp until 26.11.23, when the window's buttons left it for the title bar.)
  - **Windows gets a title bar of its own.** A 32 dp strip above the app, as other Windows apps have, replaces the system caption and follows the theme, with a hairline under it. The app's mark and name sit at its start, and minimise, maximise/restore and close sit at its end as flat 46 dp buttons, Windows' own size. The rest of the strip moves the window, and a double-click maximises it. Nothing of the window's floats in the app's own top band, and because the title bar names the app, the band carries no brand under it: its top-left corner is empty until a deck opens, when the back button fades in there, 12 dp from the deck's name. Windows keeps the rounded corners, shadow, resize edges and Snap. Other systems keep their own title bar.
  - The wide editor header is the deck's name, its colours and the legality stamp, which says *Legal* or *Draft* in words (I4). Under 1000 dp it also drops the pencil beside the name.
  - Below 840 dp, navigation is a floating pill (`PillNavBar`, after AnymeX's "Dynamic Pill" bar). It's about 88% of the screen wide, sits 12 dp above the bottom edge and is somewhat transparent, with no blur, and content scrolls underneath it. Lists add `LocalNavClearance` to their bottom padding, so their last item can scroll clear of it. The selected destination is a teal pill. **Every item shows its label**, selected or not, as Material's navigation bar does: an icon alone makes a first-time player guess (I5). It holds only the necessities:
    - **Top level:** Decks, Cards, Sync and Settings.
    - **Inside a deck:** the app's pill steps aside, and back returns to it (N3). The editor floats its own sections in the same pill: **Deck**, **Add**, **Analysis**, **Test**, and **More** for Matches and Versions, so the pill never holds more than five items.
  - On wide windows, the same pill sits at the top right with every label showing.
  - The phone editor header keeps only back, the deck name (tap to rename), its colours, the Legal or Draft stamp, and the gauge.
  - On wide windows, panel tabs such as the deck panel's sit at the top of their panel.

## 2. Speed of building

- **S1. One tap per copy.** Adding or removing a copy is a single tap or click from both the card list and the deck list. Card details never sit between the person and a quantity change. (GNDM TCG Dex shipped "Quick Connect" so cards stop needing multiple taps.)
- **S2. Tap the card for details; buttons change counts.** The card body opens details, and the − and + buttons only change quantity. The two never swap places.
- **S3. Every rule limit is visible where you act.** In the card list, a card shows how many copies are in the deck, a **Max** tag at its copy limit, and an **Off-color** tag when adding it would make a third color. The person learns the rule without trying and failing.
- **S4. Nothing blocks a save.** Illegal or incomplete decks save as drafts. Rules produce messages, never disabled buttons.
- **S5. Results update as you type.** Search and filters apply immediately, with no Search button, and the result count is always on screen.

## 3. Information and feedback

- **I1. Numbers read like instruments.** Counts and limits are written `current/limit` (`Main 42/50`) in one format across the app, with tabular figures, so totals don't jump around while you edit. A number that changes while you work is set in a face that has tabular figures; Quicksand has none, so `TabularNumbers` sets those numbers in the platform sans with `"tnum"`, keeping the size and weight of the style it is merged into.
- **I2. Deck health in one line.** Every deck header shows its legal or draft state, its main-deck count, and its colors, with no scrolling needed.
- **I3. Explain the rule with the problem.** A legality message names the card, the count and the limit (*"Inferno Lancer has 5 copies; the limit is 4"*), never just *"Invalid deck."*
- **I4. Color is never the only signal.** Card colors always come with their name or initial, and status tags always include a word. Charts tell series apart by position or label as well as colour.
- **I5. Label every icon.** An icon on its own needs a text label next to it or an accessible description plus a tooltip on desktop. Navigation is never icon-only (N5). (A reviewer criticized Aniimo's controller menu for having "the most incomprehensible buttons for any given action.")
- **I6. One icon set.** Every icon is an outline from `LineIcons`, at one stroke weight, in the colour of the text beside it; icons never get colours of their own. Sizes follow the control, not taste: 16 dp beside a label, 18 dp in a small control, 20 dp in a toolbar control and 22 dp in a phone's controls and nav, with 14 dp only inside a tag and larger ones only on a big tile or the phone's + button. A filled glyph, an emoji or a Unicode symbol standing in for an icon reads as a different product. (Three filled Material arrows and one filled glyph went on 2026-11-10.)

## 4. Calm by default

- **C1. No nagging.** No badges, banners or pop-ups announce anything the person didn't just do. Sync status appears on the Sync screen and as a small dot on its navigation item. The one exception: a sync that actually moved changes, auto-sync included, says so in a short snackbar wherever you are; syncs with nothing to move stay silent, and failures show only the dot. (Aniimo's reward systems "scream at you from multiple menus.")
- **C2. Undo instead of "Are you sure?"** Reversible actions such as deleting a deck or clearing a section happen immediately and offer **Undo** in a snackbar. A confirmation dialog is only for actions undo can't reverse, such as forgetting a paired device or overwriting from a backup. A dialog is never the first answer to a task that could happen in place.
- **C3. One bulk action instead of many single ones.** When something applies to many items, offer it once, such as *Clear section* or *Sync now*. (Aniimo reviewers asked for a claim-all.)
- **C4. Pick from options, don't type.** Wherever a value comes from a known list, such as a card name, a color, a tag already in use or a ruleset, offer it as a choice. Typing is the fallback.
- **C5. Plain words.**
  - No exclamation marks, no greeting headlines (the welcome tour opens on *Lock On* and what it is), and no words that sell, such as *seamless*, *unlock*, *supercharge* or *magic*. Say what the thing does.
  - Titles, labels, buttons and one-line notices end without a period (*Deck list copied*, *Deleted Blue Red Aggro*); sentences in body text, and status and error lines from sync, rules and the card database, keep theirs.
  - Short labels: *Search*, not *Search anything…*. A placeholder may carry the result count (S5).
  - A change says what it compares to (*+2 wins vs last version*), and a number never stands alone without its label.
  - No *New*, *Beta* or *Pro* badges. Something new is announced once in the changelog.
  - A question in a dialog's title is not asked again underneath: the line under *Delete Blue Red Aggro?* says what happens next.
  - Buttons name their action (*Compare*, *Keep both*), never *OK* or *Yes*.

## 4a. States

Every screen is checked in all of its states, not only the full, happy one (impeccable's product floor).

- **E1. Every control has its states.** Rest, hover on desktop, pressed, keyboard focus and selected, plus disabled where an action is truly impossible (S4 still applies: rules never disable a save) and loading where it waits. One state looks the same on every control: hover and press are washes (K3), focus and selection are the selection line (V1).
- **E2. Loading keeps the shape.** Cards load into their drawn placeholder (V4a) and lists keep their rows, so nothing shifts when content arrives. The reticle spinner is for a whole operation, such as a sync or a card database download, never in the middle of content.
- **E3. Empty states teach.** An empty screen says in one line what goes there and offers the action that fills it: no decks shows **New deck**, a search with no results offers to clear the filters that hid them. No filler illustration.
- **E4. Real content in every check.** Screens are reviewed with long card and deck names, a 50-card deck, a deck with nothing in it, three-colour edge cases and the error states of sync and the card database (K7).

## 5. Look and feel

Lock On follows the philosophy of Nintendo's recent menus (Switch 2 era) inside Stella Sora's dusk: simple yet functional, removing anything uncomfortable before adding anything. A quiet sky frames calm, solid panels, with rounded type and rounded rectangles, and the cards are the one bright thing on a screen. That is unslop's "quiet chrome, rich moments" and impeccable's "brand lives in precise details": the chrome uses the ink ramp and one accent, and the colour belongs to the cards. Material 3 supplies behaviour, menus, text fields and accessibility. The deck-builder patterns come from the games players praise most: Hearthstone, Marvel Snap, Legends of Runeterra and Master Duel (see [research](#research-behind-the-rules)). The shared toolkit is in `ui/components/GameUi.kt`.

- **V1. Solid panels over sky.**
  - Large panels are calm and solid (`GlassPanel`, `Modifier.glass`, named for their history): white by day and a raised step of the dusk ink at twilight. The sky frames them rather than showing through. Nothing is frosted or blurred.
  - A panel's edge is one hairline, the theme's ink at a tenth, the same all the way round (`glassColors().line`). It is an edge, not a highlight: the rim that faded from bright at the top to dim at the bottom read as glass, and went on 2026-11-10.
  - Depth is surface steps and hairlines. Only things that float over other content cast a shadow (the nav pill, the player card, menus, toasts, dialogs, the phone's + button), and the shadow is ink, soft, offset downwards, never a colour and never a halo. A thing declares its depth once: a hairline or a shadow, not a hairline under a wide shadow.
  - Fills are flat. No gradient runs from one hue to another on a panel, a bar or a button, no text is filled with a gradient, and nothing glows behind content: a card's details show the card on the plain panel. A scrim that keeps text readable over art, the sky's tone shift (V7) and the fade at the end of a sideways row are the exceptions.
  - Corners are rounded rectangles: dialogs 28 dp (Material's own), panels 22 dp, tiles, menus and notices 16 dp, and buttons, chips, tabs, search fields and icon buttons 14 dp on phones and 12 dp on desktop (`LockOnShapes.button`). Only the floating nav pill and switches keep pill ends (`LockOnShapes.pill`).
  - Nested corners add up: something 4 dp inside a rounded shape has that shape's radius less 4, never below 4, so a thumb in a track is 8 dp on desktop and 10 dp on phones (`LockOnShapes.inset`), and a menu's rows and a notice's button sit 4 dp inside their 16 dp card. (Thumbs were 11 dp everywhere, menus 22 dp and notices pills until 2026-11-10.)
  - No stock Material visuals show through. Buttons are `GameButton`, tabs `PillTabs`, chips `PillChip`, switches `PillSwitch`, segmented choices `PillSegmented`, and dialogs `GameDialog`. Each one is a skin over the Material control it replaces, not a new kind of control: it keeps that control's role, states (E1), size (R1) and accessibility semantics, so it works the way an Android or Windows user expects. A dialog's main action is the solid teal button, and it closes with a Cancel or Close button, never a corner ×.
  - **Selection:** solid teal marks navigation and main actions only (the nav pill, the editor's panel tabs, primary buttons). Every other single choice is a white thumb with teal text (`PillSegmented`, `PillTabs(soft = true)`). Filter chips that are on get a teal outline and tint.
  - **Selection outline (desktop):** one solid 2 dp line in the theme's teal, flush on the inside of the shape's edge, with no gap and no gradient. Cards, deck tiles, deck rows, printings, chips that are on, focused search fields and open versions all use it; colour dots wear it just outside the dot (`drawSelectionEdge`, `SelectionEdgeWidth`).
- **V2. Section headers.** The label in sentence case with its count beside it in the quieter ink, such as `Units 32`. No rules, sparkles or kicker labels above a heading. A count is never a tinted pill, and a tile of one number is its label and the number, with no icon, sparkline or mini chart to fill it (Analysis: Burst, Blockers, Links).
- **V2a. Type.**
  - Headings, titles, labels and buttons use Quicksand (SIL Open Font License), a rounded face that carries the world's friendliness, at one weight, 600. Quicksand is drawn light, so its 600 sits where other faces' 500 does. Body text stays the platform sans at 400 for readability. Figures that change while you work use the platform sans with tabular figures (I1).
  - Hierarchy comes from size and colour, never from a heavier weight. Nothing is above 600.
  - Sentence case everywhere. Nothing is set in capitals with wide tracking. Small text never gets negative tracking; labels may open slightly (0.2 sp).
  - No italic for emphasis in the interface.
  - Text sizes come from the Material type roles (`MaterialTheme.typography`), never hand-picked per screen, and are in sp, so they follow the system's font size.
  - (Everything in Quicksand was 700 until 2026-11-10; with every word bold, none stood out, and the chrome competed with the cards.)
- **V3. Colour roles.**
  - **The neutrals are one ink ramp tinted toward the dusk.** Ground, panels, raised steps, hairlines and text all come from it. Light mode is a pale, quiet sky; dark mode is a deep dusk, never pure black. The tint is kept low so a surface reads as night or day first and blue second: at most 0.03 OKLCH chroma for the dark ground and panels, and 0.015 for the light ones. Cards lie directly on the sky, so it stays low in colour and well away from the card faces' brightness. (Why navy and not grey is in [Where the guides disagree](#where-the-guides-disagree).)
  - **Three text roles:** primary (`onSurface`), secondary (`onSurfaceVariant`) and muted (`MutedInk`). Muted is for meta and captions only and still reaches 4.5:1. No fourth near-identical tint.
  - **Primary (teal)** is the one accent of the chrome. It means "you can act" or "selected": primary buttons, the selected tab, chip, segment and destination, **+**, and counts in the deck. It covers a few percent of a screen, never a whole region.
  - **Secondary (sky blue)** is for information, such as an info notice or a link, and never fills a control. Charts are one slate ramp (`ChartSlate`), so a bar never reads as a card colour or an action.
  - **Tertiary (soft gold)** is for celebration only: a full gauge, a finished sync and the Lock On moment. There is one gold, #FFC34D, for every fill and for text at twilight. Text on a day panel uses the darker #9A6200, because the gold can't carry text on white.
  - **Errors** keep Material's error role, toned to sit in the palette.
  - **Card colours** are for card identity: cost orbs, the card faces and a deck tile's colour tint. They never colour chrome.
  - **Violet** is the logo's alone (V9), so it never reads as a button.
  - A full deck that needs review holds its gauge in slate rather than gold.
- **V3a. Material You colours, on by default.** Where the system has an accent colour, Lock On's palette comes from it: the wallpaper's on Android 12 and later, and the accent colour on Windows, macOS and GNOME. Settings › Appearance › **Material You colors** turns it off and brings back Sky and Twilight's own teal and dusk; the Theme choice still picks day or night either way. It changes hue, never the recipe (`systemColorScheme`):
  - **The neutrals** keep Sky and Twilight's lightness steps for the ground, sky, panels, raised steps, hairlines and text, tinted toward the accent's hue within V3's caps (0.03 OKLCH chroma at twilight, 0.015 by day). Warm hues (reds, pinks, oranges, yellows) tint at a little over half that, since at the same chroma they read as a maroon or olive sky rather than a quiet one. The sky stays quiet and the cards stay the brightest thing on a screen.
  - **Primary** takes the accent's hue in place of teal, keeps teal's role (V3) and its contrast: its chroma is held between 0.08 and 0.14, and its lightness moves until its label clears 4.5:1, so a yellow accent gives a deep ochre button rather than an unreadable one.
  - **Meanings stay put.** Information blue, celebration gold, the error red, the card colours, the chart slates and the logo's violet don't follow the accent, because each one says something.
  - **No accent, no change.** Android before 12, a system that names no accent, or an accent too grey to act as one (below 0.035 chroma, such as macOS's graphite) shows Sky and Twilight's own colours, and Settings says why under the switch.
  - **It follows the system.** The accent is read at launch and whenever Lock On comes back to the front, and a change crossfades like a change of theme (V5). The launch screen keeps its own violet sky (V7).
- **V4. Card faces carry identity at a glance.** A tile always shows its cost (top-left). Without artwork it also shows its colour band, level and type initial. With artwork, the image sits over that placeholder, so a slow or broken link never leaves a blank.
- **V4a. Cards are never cropped where they are the subject.**
  - Grid tiles, the test hand, card details and printings show the whole card at its real 63:88 shape, with high-quality filtering. Cost and AP/HP are never covered.
  - The test hand's fan is the one place cards overlap, because a held hand reads as a hand. Each card's cost stays in view, the card under the pointer comes to the front, and a tap opens any card whole.
  - The editor banner is the exception, with a crop of a card's art as a backdrop. Deck strips and deck tiles show a small crop or the cover card, and the whole card is always one tap away.
  - Names aren't repeated under cards that already print them.
  - Cards without artwork get a drawn placeholder in the same shape, so grids never shift when images load.
- **V5. Motion explains and gives feedback, quietly.** Every animation answers *what changed*, *where did I go* or *what can I touch*, and none of it competes with the cards. Short distances, small scales, no bounce or overshoot, and nothing shimmers, pulses or sparkles: every spring is critically damped, the switch's knob included. Timing lives in `Motion.kt`.
  - **Timing:** 120 ms for small exits, 200 ms for most changes, 280 ms for larger moves, all on Material 3's emphasized curves, one family for the whole app. Exits are faster than entrances. Items in a group start 30 ms apart.
  - **What moves:** transforms, opacity and colour. Width, height and position changes in layout glide only through the list and reveal helpers below, never by animating a size by hand.
  - **Navigation:** going deeper drifts a short way forward, coming back drifts back, and switching tabs fades through. Nothing plays on app start beyond the boot screen; the app loads straight into the task.
  - **Arriving and revealing:** dialogs and popups fade in while growing from just under their size (`Modifier.arrive`). Banners and hints unfold into place and fold away (`revealIn` / `revealOut`), so what is below them glides instead of jumping. Swapped content, such as a stamp or a pair of buttons, fades through.
  - **States warm, never switch:** a gauge warms from teal to gold to red, and a picked item's light moves to it.
  - **Lists:** rows glide when cards are added, removed or regrouped.
  - **Numbers:** they roll up a short way when they grow and down when they shrink. A count that grows also nudges up in size by a few percent.
  - **Adding and removing:** the card or strip wears a solid 2 dp line, the same as selection, for about a third of a second: teal when a copy is added, red when one is removed. The count says the rest.
  - **Hover (desktop):** cards rise a few pixels and wear the selection line, with no shadow and no change of size. The card's drawing rises, not its hit area, so a pointer near the edge never flickers between hover and rest. Strips ease into their art. Buttons brighten a touch, and rows and tiles take a 4% ink wash. Resting on a card for a moment opens the whole card beside it, never over it.
  - **Charts:** bars grow into place.
  - **Test hand:** cards fade in and settle up a short way, and shields flip over when revealed.
  - **Touch:** one answer everywhere, `PressWash`: a flat 8% ink wash over the thing's own shape, on at once and gone 120 ms after letting go, with no ripple, no circle and nothing that follows the finger. Tiles, cards and buttons also sink a hair. Keyboard focus is the selection line. Nothing on touch glows. Holding the export preview to open it uses the same wash and sink. A card held up to inspect is the one thing that tilts, because it follows the finger, with one solid rim in the card's colour. Phones give a light tick on each +/−.
  - **The Lock On moment** is the app's one authored animation and its signature: when a deck becomes legal, the brackets close on the reticle, a soft glint crosses the editor banner and its rim warms to gold, then fades. On phones, a confirming haptic plays. It plays only on the change, never on opening a legal deck, and the glint appears nowhere else.
  - **Loops:** nothing loops except progress, and progress uses the reticle spinner, or on a device tile that is syncing or sending, a band of light running round its rim. The sky is static.
  - **Reduced motion:** with the system's "Remove animations" setting on, every animation becomes an instant change or a very short fade. Tilt, bumps, the add and remove line and the glint are skipped, but colour and state changes that confirm an action still show.
- **V6. Dark and light are equals.** Both themes use the same colour roles, and each is composed on its own, never inverted from the other. Contrast is at least 4.5:1 for text and 3:1 for large text, controls, icons, focus lines and tags, including text over the sky and over card art.
- **V7. Decoration is never the only signal, and rarely there at all.**
  - **The sky:** one quiet gradient from top to bottom, a tone shift within one colour of the ink ramp: pale grey-blue by day, deep dusk at twilight. Nothing glows or floats in it. (Its teal, blush, violet and gold glows and the diagonal beam were turned down on 2026-11-05 so cards stand out, and removed on 2026-11-10: colour washes behind content compete with the cards, which are the one bright thing on a screen.)
  - **The Lock On mark:** a bold ring with four short studs and a big center dot, all in one colour, thick enough to hold at 16 px. The app icon is flat: the mark in white on a single violet tile, with no gradient, glow or shading. The mark appears in the player card's badge, on face-down shields and as the launcher icon.
  - **States that need attention,** a deck conflict and a failed sync, use the error role's tint, its icon and words that explain them (I4, T3). No coloured edge bar or stripe pattern marks them (K8).
  - All of these are original shapes, not official logos or artwork.
- **V8. Game deck-builder patterns.**
  - **Deck cards:** the deck list shows each card small and whole, at its real shape, grouped by type. A − count + plate sits underneath. Desktop fits as many 104 dp cards as the panel holds, with 40 dp targets. Phones show cards three across from about 390 dp, with 48 dp targets (R1).
  - **Card view** is the default, as the line above describes; **Show as list** shows the deck as strips instead: the view toggle beside the deck list's first heading on wide windows, or holding **Deck** in the phone's bar. Settings' **Open decks as card images** picks which view decks open in. Each strip has a cost orb in the card's colour, a small crop of the art, the name and − / +. On desktop strips lie flat on the deck panel like the card list's rows, with no fill or rim of their own, and take the 4% ink wash on hover; on phones, where the list sits on the sky, each strip is on solid glass like its heading. Their − count + plate is the neutral fill of K3, the same as in the card list. (Strips were a saturated indigo bar with a white rim until 2026-11-15.) The side deck always uses strips, since its rows need the **To deck** action. Every view toggle shows cards first and the list second, since cards are the default.
  - **Counts:** the plate shows one number, teal in the deck, muted at 0, red past the limit; the card list shows Max as quiet text in the card's name strip. (Copy pips, as in Runeterra, drew every count a second time as dots and went in the UI polish pass of 2026-11-15.)
  - **Gauges:** `Main 42/50` is a rounded bar, the one progress shape for a single goal. They're teal while filling, gold when exactly full, and red when over. Nothing is drawn on the bar itself (K8).
  - **Level curve** (as in MTG Arena): a compact curve sits at the top of the deck list.
  - **Analysis:** one main chart leads and is the biggest thing in the tab; the rest are smaller and of a different kind, such as number tiles or a short ranked list, never a second chart of the same size. Bars are slate, on a muted grid with muted labels.
  - **Deck tiles:** phones and wide windows share one layout: the cover card at the side, then the name and colors, the state and record, and the gauge. The card is zoomed in on its illustration, a little less on wide windows. Wide windows fit as many 320 dp tiles to a row as they can and add the age, and the record is first to give way if that line runs out of room. Without art, the card's space shows the deck's colors in a calm flat tint, split evenly if it has two, and on wide windows a shield.
  - **Hero banner:** the editor header is a solid strip with the cover art fading in from the right under a scrim, so the deck's name always sits on calm space.
- **V9. Navigation floats and fits the theme** (as on Stella Sora's home screen, N5).
  - **The brand** is the Lock On mark on a flat, rounded logo tile beside the name, shown once per window: in the title bar where Lock On draws its own (Windows), and at the top-left of the band everywhere else. Inside a deck it gives way to the back button. The tile follows the theme: violet with a white mark by day, pale lilac with a deep violet-navy mark at twilight. Violet is the logo's alone in the header, so it never reads as a teal button. Wide screens carry no page title beside the nav; the lit item names the screen.
  - **The pill** (N5): destinations float in a translucent capsule, and the selected one is a teal pill with its label. Its translucency has a job, showing that content continues under it, so it carries no blur or glow.
  - **Window controls** (N5, desktop) live in the title bar, not beside the pill: three flat buttons the same size, with drawn glyphs and a label and tooltip on each (I5). As on Windows itself, close shows its weight by turning red on hover, not by size. They tint on hover and darker when pressed.

## 5a. Consistency (checked on every change)

These came from a UI review on 2026-10-28, where a sparkle on full gauges, an edge bar on the selected folder, a bare back arrow and window controls shorter than the nav pill all read as mistakes. Design-audit checklists agree on the same causes: values off the scale ("magic numbers"), similar elements styled differently, and decoration the eye reads as UI ([design audit](https://skills.sh/devanshudesai/agent-skills/design-audit)). Windows keeps its caption buttons one size and always visible ([title bar design](https://learn.microsoft.com/en-us/WINDOWS/APPS/design/basics/titlebar-design)). unslop and impeccable both put the same checks first: if the save button looks different in two places, one of them is wrong.

- **K1. One height per row.** Buttons, fields and icon buttons that share a row are `ControlSize` (44 dp on phones, 40 dp on desktop). Capsules side by side share a height. The desktop's nav pill and back button are `ControlSize` too.
- **K2. Same rank, same size.** Controls of equal rank are equal in size. Rank shows through fill (teal, panel, text) and labels, never through sizes that drift apart.
- **K3. One control recipe on desktop.** Two heights: `ControlSize` (40 dp, 12 dp corners) for toolbars and buttons, `SmallControlSize` (32 dp, 10 dp corners) for chips and small icon buttons. Everything that isn't teal is one flat wash of ink (`neutralWash`, or `neutralFill` where it may sit on the sky), with no rim unless selected or focused. Hover is one step of fill and press another; nothing else changes. A thumb or button inside a track sits 4 dp in with 8 dp corners. The card list and the deck panel share one stepper: a control-height plate with − count + inside. Phones keep their own sizes.
- **K4. Icon-only actions have a body.** Back, Export, ⋮ and similar use `GlassIconButton`, a rounded square, never a bare glyph. Back sits 12–16 dp from the title beside it.
- **K5. Let the layout explain.** No subtitle under a screen title (`ScreenHeader` has none), no description under a setting whose name and options already say it, and one heading per group, not a heading over a single label. Words appear for news (an update, a pending name), problems (I3) and trust (T2), not to narrate what is on screen.
- **K6. Spacing on a 4 dp grid.** Gaps are 4, 8, 12, 16 or 24 dp, and similar gaps match across screens. Fixed heights and widths are multiples of 4 too. A 2 dp step is for nudges inside a small control. An odd value stays only where it centres or lines something up, or inside a drawn mark (the reticle, a card's face, a stroke). The saved deck image keeps its own measurements, which place counts against the card art. Space shows grouping: the gap inside a group is smaller than the gap between groups, and a heading sits closer to what it heads than to what is above it. Icons, labels and numbers each line up in one column. (An audit on 2026-11-10 found about a hundred gaps of 6, 10 and 14 dp; they moved to 8, 12 and 16.)
- **K7. Check in pictures.** Every UI change is reviewed in `ScreenshotTest` renders (`:desktopApp:previews --tests`, not part of `test`), in both themes, on phone and desktop, with real content (E4), before it ships. Phone changes are also checked at a system font size of 130%, where fixed layouts clip.
- **K8. No stray marks.** Selection is a fill or the selection line, never an edge bar or underline. No coloured bar or stripe runs down the side of a card, row, banner or notice. No rule, sparkle or glow sits where it could be read as a control or a state: no lines under banners, nothing drawn on a gauge.
- **K9. The parts we didn't draw carry the theme too.** Text selection, the text cursor, scrollbars and keyboard focus take their colours from the palette, not from the platform's defaults.

## 6. Reach and input

- **R1. 48 dp touch targets** on Android, including quantity buttons, even when they look smaller, with at least 8 dp between neighbouring targets.
- **R2. Keyboard first on desktop.** `Ctrl+F` focuses search, `Esc` goes back, and `+` / `−` change the focused card's count. Every action also works with a mouse. A shortcut shown in the interface is a small keycap, like the `Ctrl F` in the search field.
- **R3. Thumb zone on phones.** Navigation, including the editor's sections, and primary actions (New deck, Keep / Draw, Sync now) sit in the lower half of the screen. A switch for a screen's own content (Sync's Devices / Send decks) sits directly above that content; navigation and the primary action stay in the lower half.
- **R4. Offline always.** Nothing needs a network connection except syncing, and a failed sync never blocks editing.
- **R5. Edge to edge on phones.** The sky runs under the status and navigation bars, and content respects the system insets, the display cutout and the keyboard, so nothing hides behind them.

## 7. Trust (sync and data)

- **T1. Never silently discard an edit.** Conflicting versions are kept and compared side by side, and the person chooses.
- **T2. Show where data lives.** The Sync screen states that only decks sync and that no account or server is involved.
- **T3. Errors say what to do next.** *"Could not reach the desktop. Check that Lock On is open on it…"*, not error codes.
- **T4. No ads and no upsell.** These are the most common complaints in Gundam deck-builder store reviews.
- **T5. Automatic sync is quiet.** Auto-sync only runs while the app is open. It never shows a snackbar or dialog. It reports itself only on the Sync screen, as *Live* or *Waiting for the desktop*. An unreachable desktop is normal, not an error, so it never sets the error dot. Errors are for syncs the person asked for.
- **T6. Seeing is not trusting.** Devices found on the network are listed with one tap to connect, but nothing pairs until a person on the computer confirms a 6-digit number that matches the phone's. Codes from QR or typing are optional extras, never the only safeguard.

---

## Where the guides disagree

Each line names what a guide asks, what Lock On does, and why. Where the guides clearly win, the rule above has already changed and the app work is in [Known gaps](#known-gaps).

- **Navy dusk or grey surfaces (V3, V7). Kept navy, made quieter.** unslop wants app backgrounds as grey as its graphite ramp and calls a screen whose ground reads as blue "too far"; it also says a ramp can be tinted toward the world as long as the saturation stays very low, and gives its ink ramp to "night, sky, water". impeccable says the brief wins over a pattern warning, that a tinted ground is a legitimate part of an Operate palette, and that "a grey screen with one accent colour is the category default". The dusk sky is Lock On's world, so the hue stays. What unslop is right about is the amount: the dusk ground was about 0.041 OKLCH chroma where unslop's ink ramp is about 0.015, and the cards, which lie on it, are what should carry colour. V3 caps it at 0.03 (since 2026-11-15 the ground `#1A1F2D` and panels `#232836` sit at about 0.027, and the day ground `#E9F1F7` at 0.012), and "never black" now means never pure black, not "always visibly blue".
- **Cards on the sky or on panels (N4, V3).** unslop puts content on surfaces from the ramp; the old rules said both "cards lie directly on the sky" and "pages on the sky put content on solid panels". Now cards lie on the sky, because they are the bright moment and a panel would only add a frame, and everything else that sits on the sky (text, settings, controls) goes on a solid panel. This is why the sky must stay quiet.
- **Quicksand in the chrome (V2a).** impeccable's product rules ban display faces in labels, buttons and data, and unslop picks plain sans faces with weights of 400 to 500. Quicksand is part of the identity (Stella Sora's and Nintendo's rounded menus) and impeccable lets a committed face stand, so it stays for headings, labels and buttons at 600, which is light for its size. Data is the exception both guides are right about: Quicksand has no tabular figures, so changing numbers move to the platform sans (I1).
- **Skinned controls or stock Material (V1).** impeccable's Android rules say to use Material components and never invent equivalents. Lock On draws its own buttons, tabs, chips and switches for the look, so the rule now says each one is a skin over the Material control it replaces, with the same role, states, size and semantics. Material's dynamic colour is used for hue only (V3a): the system's accent replaces teal and tints the neutrals, but Lock On keeps its own lightness steps, chroma caps and fixed meaning colours, because those, not the hue, are what keep the cards the bright thing on screen. Before 2026-11-15 it wasn't used at all.
- **Wide-window navigation (N5).** impeccable expects a navigation rail or drawer on wide screens. With four destinations, a labelled pill at the top right is as clear, and it gives the width to the card list; the rule is that it changes shape and shows every label, not that it stays a phone bar.
- **Icon-only phone navigation (N5, I5). Changed.** impeccable flags icon-only navigation as a first-timer's red flag, and Material's navigation bar labels every destination. The phone pill now labels every item.
- **Corner sizes (V1).** unslop's panels are 14 to 20 and its cards 12 to 16. Lock On's 22 dp panels and 28 dp dialogs (Material's own) are part of the rounded, Nintendo-like character and keep the nested-radius rule, so they stay.
- **More than one accent (V3).** unslop allows one accent in the chrome. Teal is that accent. Gold appears only for celebration and sky blue only for information, each rare and tied to one meaning, so neither competes with teal.
- **Icon sizes (I6).** unslop wants one icon size everywhere. Lock On ties each size to the control it sits in, with one stroke and one set, which keeps the same consistency across very different control heights on phone and desktop.
- **Caution stripes (V7, K8). Retired.** Both guides argue against them: impeccable bans coloured side stripes on banners and notices and patterned backgrounds a world didn't choose, and unslop removes decoration that isn't the product. They were the last piece of the cockpit look dropped on 2026-10-26, a 10 dp stripe down the side of the conflict banner and the failed-sync status, and went on 2026-11-15. The error tint, icon and words say it.
- **Gradient app icon (V7). Changed to flat.** unslop names the gradient tile as one of the first things that reads as generated, even in a logo. The tile is one solid violet, #8A5EE1, in the launcher, the desktop icon and the header (pale lilac #C5B0FC at twilight).
- **The "frosted" banner (V8). Reworded.** impeccable and unslop both reject glass as decoration. The banner never used blur; it is now described as what it is, a solid strip with a scrim.
- **The glint (V5).** unslop removes shimmer, and impeccable asks for one authored moment. The glint stays because it is that moment: it plays once, on the change to legal, and nowhere else.
- **Hover lift (V5).** Both guides warn against things moving on hover. Cards rise a few pixels, which unslop allows, and impeccable's condition is now in the rule: the drawing moves, not the hit area.

## Known gaps

The rules above are ahead of the app in these places. Each is a follow-up change, reviewed in pictures (K7).

None at the moment. The last six (dusk chroma, tabular figures, caution stripes, phone navigation labels, the launcher icon and the platform surfaces) were closed on 2026-11-15.

## Research behind the rules

**Craft guides**

- **unslop (Damir Sokolovsky, [heydamir/unslop](https://github.com/heydamir/unslop), CC BY 4.0)** turns generated-looking UI into a designed one by fixing spacing, colour, icons, copy, type, layout, details, visuals and motion, in that order, after writing a five-line direction.
  - Taken into Lock On: the direction (section 0) and the template check, quiet chrome with the colour in the cards, one tinted neutral ramp with a low chroma cap and three text roles (V3), one accent, no hue gradients, gradient text or glows (V1), flat logo (V7), sentence case, 400 to 600 weights and no italic (V2a), plain copy and no badges (C5), spacing that shows grouping (K6), charts with one lead (V8) and motion that never loops or bounces (V5).
- **impeccable (Paul Bakaus, [pbakaus/impeccable](https://github.com/pbakaus/impeccable), Apache 2.0)** is a design skill with a craft floor, product (Operate) rules, Android rules, and critique heuristics.
  - Taken into Lock On: Operate mode and "the brief wins" (section 0, and the navy choice), the states floor (section 4a), no modal as the first answer (C2), no side stripes or decorative glass (K8, V1), skinned controls that keep Material's semantics (V1), labelled navigation (N5), predictive back (N3), 8 dp between targets (R1), edge-to-edge insets (R5), sp type from the Material roles (V2a), themed platform surfaces (K9), a still hit area on hover and exits faster than entrances (V5), and the font-scale check (K7).

**Inspiration games**

- **Stella Sora (Yostar).** Reviewers praise a clean, modern, intuitive UI with clear labels, few nested menus and very smooth animation ([Game8 review](https://game8.co/articles/reviews/stella-sora-review)). Its "retro-future" look mixes fantasy characters with 90s-style tech, and its menus are compared to Persona 5 ([TapTap review](https://www.taptap.io/post/10565172)). Its run-based *Potentials* system offers a choice of three and saves good builds as reusable *Records* ([Game8](https://game8.co/articles/reviews/stella-sora-review), [Oslink guide](https://www.oslink.io/blog/guide/stella-sora-beginner-guide.html)).
  - Taken into Lock On: the dusk world (section 0), stylised details (V2, V7), shallow navigation (N2), clear labels (I5), purposeful motion (V5), and the sample hand's "draw and judge" loop.
- **Aniimo (Pawprint Studio).** One review praises a UI that is "clean and fast in design" with "clear and precise information", including showing which creature suits gliding, swimming or climbing ([GamingOnPhone](https://gamingonphone.com/reviews/aniimo-game-review/)). Others criticize a 16-icon menu, reward systems that "scream at you from multiple menus", constant interruptions, unclear button prompts, and having no claim-all ([Siliconera](https://www.siliconera.com/review-aniimo-is-a-constantly-distracting-experience/), [Lootlevelchill](https://lootlevelchill.com/reviews/aniimo-review/), [Dualshockers](https://www.dualshockers.com/aniimo-review/)).
  - Taken into Lock On: suitability tags (S3, from the gliding/swimming hints), and the anti-patterns behind N1, C1, C3 and I5.

**Game deck builders (for V1, V5 and V8)**

No published UI teardown of these deck builders turned up, so where no source is linked, the pattern comes from how the game behaves.

- **Hearthstone (Blizzard)** is widely called the friendliest digital card game UI, built to feel like a tactile physical box ([GDC 2015, "Hearthstone: How to Create an Immersive User Interface"](https://gdcvault.com/play/1022036/Hearthstone-How-to-Create-an), [collection manager](https://hearthstone.wiki.gg/wiki/Collection)).
  - Taken into Lock On: deck-list card strips with a cost gem and an art crop, hover lift, and deck boxes.
- **Marvel Snap (Second Dinner)** is praised for a clean mobile interface where every tap gets visual feedback ([MobileSyrup](https://mobilesyrup.com/2022/10/29/marvel-snap-mobile-game-review/), [GamersDecide](https://gamersdecide.com/articles/marvel-snap-review)).
  - Taken into Lock On: press and hover feedback, add and remove lines, and counts that nudge when they grow.
- **Legends of Runeterra (Riot)** shows copies as pips and lets region colours drive a deck's identity ([patch 0.9.1](https://invenglobal.com/articles/10550/patch-091-speeds-up-animations-introduces-jinx-and-garen-themed-boards-to-legends-of-runeterra), [deck building](https://invenglobal.com/articles/9832/guide-how-do-you-build-a-deck-in-legends-of-runeterra)).
  - Taken into Lock On: colour orbs and the card's own colour as its identity. (Copy pips went on 2026-11-15, V8.)
- **Yu-Gi-Oh! Master Duel (Konami)** is praised for fast filtering across thousands of cards ([Seafoam Gaming](https://seafoamgaming.com/2022/02/07/yu-gi-oh-master-duel-ios-xbox-series-x-switch-eshop-review/)).
  - Lock On already followed this with S5.
- **MTG Arena (Wizards)** is the counterexample, criticised for an editor where the big picture is hard to see ([Giant Bomb](https://giantbomb.com/users/7077/articles/some-thoughts-on-magic-the-gathering-arena)).
  - Taken into Lock On: its level curve beside the deck list.
- **Mecha HUDs:** *Gundam: Senjou no Kizuna II*'s cockpit HUD ([HUDS+GUIS](https://www.hudsandguis.com/home/2024/gundam-arcade)) and Hawken's grid-based HUDs with a single yellow accent ([HUDS+GUIS](https://www.hudsandguis.com/2013/11/27/hawken-mech-huds/)).
  - Tried first as a dark cockpit look and replaced on 2026-10-26: it read as heavy and didn't fit. Only the Lock On reticle and red kept for errors remain; the caution stripes that also stayed are retired by V7.
- **Stella Sora's home screen** (see above) became the model for the current look: sky, solid panels, pills and a floating player card (V1, V9). The nav bar follows AnymeX's floating "Dynamic Pill" ([source](https://github.com/RyanYuuki/AnymeX/blob/main/lib/widgets/common/navbar/styles/floating_pill_navbar.dart)) (N5).

**Other Gundam Card Game builders**

- **ExBurst.dev** ([deck builder](https://exburst.dev/gundam/deckbuilder)) puts the card list on the left with grid/list views, − / + and quick 1–4 buttons, and the deck on the right.
  - Sorting by level, ID, name, cost, AP, HP, price and rarity, with a group-by-type view.
  - Deck tabs for Stats, Considering, Notes, W/L% and Versions, plus a Warnings tab.
  - On mobile, Cards / Deck (n/50) / Warnings tabs, and keyboard shortcuts.
- **GNDM TCG Dex** ([App Store](https://apps.apple.com/us/app/gndm-tcg-dex/id6746962562)) has a card database with many filters, main and resource deck validation, deck links, text export, and a collection with prices.
  - Reviews complain about full-screen ads, restrictive search, stale card data and collection-filter bugs.
- **TCG Stacked** ([deck builder](https://www.tcgstacked.com/gundam/deck-builder)) groups by type with counts, and has deck price, format validation and export. It has no playtesting.
- **The official Teaching App** ([App Store](https://apps.apple.com/us/app/-/id6745876331)) builds decks by picking a deck color first.

**What players want, from listings, store reviews and feature sets.** Reddit couldn't be read directly, so this comes from what the tools compete on and what their reviews ask for:
- fast adding, with no multi-tap
- validation for both decks
- type grouping and sorting
- curve and type stats
- notes, versions, and a "considering" list
- text and link sharing
- a test hand, which none of the builders above offer
- collection and prices (planned later)
- no ads

**Official setup rules used by the sample hand.** Draw 5, with one redraw that sends the hand to the bottom of the deck and draws 5 new cards. Then place the top 6 cards as shields, plus an EX Base, and an EX Resource for Player Two ([rules Q&A](https://gundam-gcg.com/en/pdf/rule-qa_en.pdf), [Play Guide](https://www.gundam-gcg.com/en/welcome/playguide.php)).
