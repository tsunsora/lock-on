# Lock On UI plan

What the interface is built from, how each screen moves, and the performance rules that keep it smooth. The rules every screen follows are in [UX_RULES.md](UX_RULES.md) and the screens themselves in [UX_MAP.md](UX_MAP.md). This file is the working plan: what is done and what is next. Last reviewed 2026-11-03.

## 1. Assets

Almost everything is drawn in code, so the app ships few files and nothing has to load before a screen can draw.

| Asset | Where | How it's used |
|---|---|---|
| Quicksand (variable, SIL OFL) | `shared/src/commonMain/composeResources/font/quicksand.ttf` | Headings, labels, buttons and numbers at 600 weight (V2a). Body text stays the platform sans. |
| App icon | `desktopApp/icons/lock-on.png`, `.ico`, `.icns`; `androidApp/src/main/res/drawable/ic_launcher_*.xml` | The flat Lock On mark on a violet tile (V7), made by `scripts/make_icons.py`. Android has adaptive and monochrome vectors. |
| Window icon | `desktopApp/src/main/resources/lock-on-window.png` | Taskbar and Alt-Tab on Windows. |
| Sample catalog | `composeResources/files/catalog/sample-catalog.json` | 20 fictional cards shown until the real database is downloaded. |
| Rulesets | `composeResources/files/rulesets/rulesets.json` | Deck size, colours, copy limits and bans. |
| Card artwork | downloaded per device into the artwork folder (WebP, 600×838) | Decoded at the size shown and kept in Coil's memory cache by size (`SizedArtworkKeyer`). Never synced. |
| Icons | `LineIcons` in `ui/components/LineIcons.kt` | One outline set drawn in code on a 24-unit grid with a 1.9-unit round stroke. Nothing else supplies an icon: the Material icons library is no longer a dependency. |
| Drawn decoration | `Sky.kt`, `SuitDecor.kt`, `Motion.kt`, `CardVisuals.kt` | The sky is painted once off the main thread into a bitmap. The reticle, Lock On mark, caution stripes, cost orbs and card placeholders are Canvas drawings. Decoration no screen uses is deleted rather than kept: the lit-column bar chart, the tricolour stripe, the armour panel lines and the sky's bokeh went on 2026-11-10. |

**Plan for assets**
- Keep new decoration drawn in code, following V7. Add a file only when a shape can't be drawn, and give it a source script under `scripts/` like the icons.
- No new fonts. Quicksand's variable axis already covers every weight used.
- Card art stays sized at decode. A new view that shows art at a new size gets its own size bucket, never a full-size decode scaled down.

## 2. Motion by screen

✓ already moved before this review · **new** added on 2026-11-03 · ◻ still to do.

### Everywhere
- ✓ Tabs fade through, deck editors drift in, and the conflict screen slides in.
- ✓ Buttons, chips, tabs, segmented pills, switches and the nav pill. Every press sinks and every selection glides.
- **new** Dialogs and popups arrive: they fade in and grow from 94% (`rememberArrival` + `Modifier.arrive`). This covers card details, the export dialog, the deck text editor, an incoming send and the hover preview (97%). `GameDialog` and the card inspector already did.
- **new** Phones: the app's pill sinks away as a deck opens while the editor's pill fades in, so there is never a moment without one.
- **new** Banners and hints unfold into place and fold away (`revealIn` / `revealOut`).
- **new** Counts in section pills roll, like every other count (`CountPill`).
- **new** Dialogs close: the last frame fades and shrinks slightly while the dim, which now eases in and out too, lifts behind it (`DialogStage`, desktop). No caller changes, and nothing is left to click twice. A change of theme fades across the same way.
- **new** `GameButton` eases between enabled and disabled, between styles, and on hover; a label of another width eases too.

- **new** Left instant on purpose: typing, search-result counts and filtering (results must feel live), scroll position, layer order, a Material menu's check (the menu closes on the pick), and anything that follows a window resize.

### Decks
- ✓ Deck boxes glide, and the selected folder's fill fades.
- **new** When a drag starts, phones fade from the folder tabs to the chip row. *No folder*, the folder header and the empty note unfold; the held deck and the ghost plate fade.

### Deck editor
- ✓ Rows and strips glide. The Lock On moment, the gauges' fill and count bump, add and remove tints, and the split handle.
- **new** Section headers, the empty states, the side deck header, the stats bar and notes glide with the cards instead of popping. *Add notes* fades into the field.
- **new** The conflict banner and the wide problem bar unfold.
- **new** The Legal/Draft stamp fades over as the deck locks on.
- **new** Gauges warm from teal to gold at exactly 50 and to red when over, instead of switching (one animated tone per gauge).
- **new** The level curve grows from the floor when it first shows (V5: charts).
- **new** Copy pips fill one at a time: one drawing with one moving number per row, so a grid pays nothing for them at rest (`drawSettled`).
- **new** Card tiles' in-deck outline, the + and − buttons' states, the deck strip's hover rim and the Off-color, Side and Deck tags all ease. `Settle.kt` holds a plain number at rest and starts its one coroutine only on a change, and a reused tile starts at rest.

### Test hand
- ✓ Cards deal in, and shields deal in and flip.
- **new** The redraw hint folds away on Keep, the shields section unfolds, and Keep/Redraw fade into Draw.

### Matches and Versions
- ✓ Rows glide.
- **new** *By opponent* and the Games header glide, and the empty hint folds away on the first game.

### Cards
- ✓ Filters drawer and its rows, results fading in, and back to top.
- **new** Grid ↔ list, and results ↔ *No cards match*, fade through.
- **new** On wide windows, the details panel fades from one card to the next.
- **new** In card details, the picked printing's light and check move across instead of switching.

### Sync
- ✓ Pages, tiles dealing in, the working rim, the transfer bar and the beacon.
- **new** The *Done* badge settles in without overshoot and is instant with reduced motion (it bounced before, against V5).
- **new** Device status lines, their trailing controls, the transfer bar and its tone fade across (`FadeSwap`, `RevealValue`). Devices open and close as they come and go (`RevealItems`).

### Settings
- **new** The rail's light moves down the groups as the page scrolls.
- **new** App update: each stage fades into the next and the download uses the transfer bar.

### Welcome tour
- ✓ Stops slide.
- **new** The step pill glides along the dots. The stop's height change now respects reduced motion.
- **new** The download, spinner and *Installed* states fade across.

### Kept as they are
- The beacon's slow turn and the switch knob's slight settle are already in UX_MAP and in `glide()`'s documentation. They stay.
- The drag ghosts' small tilt stays. It reads as a picked-up card, not decoration.

## 3. Performance rules

These come from an audit on 2026-11-03. On desktop every coroutine started in composition is posted through Swing's event queue, and every frame redraws the whole window through Direct3D. So the expensive things are per-item coroutines and frames nobody asked for.

- **P1. Loops stop off screen.** A kept tab or editor is composed but not drawn. An infinite animation there still asks for a frame every vsync, so the window never idles. Loops read `LocalScreenShown` and hold still while it is false. *(Done for the Sync beacon.)*
- **P2. No coroutine per lazy item at rest.**
  - Effects that only react to a change (bump, flash, press) are `Modifier.Node`s that launch only when the value changes.
  - `composed {}`, `LaunchedEffect` and `animate*AsState` in grid tiles and list rows are the exception, not the default.
  - *(Done: `bumpOnIncrease` is now a node, and `pressScale` no longer launches on reuse when it is already at rest.)*
- **P3. Animated values are read where they are drawn.** Animated values go into `graphicsLayer {}`, `drawBehind {}` or a layer's place block, not into `Modifier.alpha(x)`, `.background(color)`, `.offset(x.dp)` or `fillMaxHeight(fraction)`. *(Done for the phone header gauge, the printing tiles and the settings rail.)*
- **P4. Lazy items have stable keys and a `contentType`.** Card rows in the deck are keyed by position, so adding one card no longer rebuilds every row after it. Mixed lists name their item types. *(Done for the deck list.)*
- **P5. Big lists are shaped once, off the main thread.**
  - The shared `liveDecks` flow decodes off the main thread and now drops the duplicate list each save produced (one per table).
  - The Sync tab reads `liveDecks` instead of decoding every deck on the UI thread.
  - Search sorts once per order and only filters per keystroke.
  - The library's filtered list and the card effect text are remembered, and the effect-text regex is compiled once.
- **P6. Constant transparency doesn't take an offscreen layer.** Side deck strips use `CompositingStrategy.ModulateAlpha`.
- **P7. Measure in the real window.** `-Plockon.scrollProbe`, `-Plockon.deckOpenProbe` and `-Plockon.bootProbe` time real frames. The off-screen `ScrollBenchmarkTest` misses the Swing and Direct3D costs above.

### Next, by expected gain
1. `CardArtwork` starts a placeholder timer coroutine for every image, even ones already in memory. Check Coil's memory cache first, or read the painter's state, so a cached image starts no coroutine.
2. Phone editor tabs (Deck ↔ Add cards) rebuild the whole grid or list on every switch and lose their scroll position. Keep pages 0 and 1 composed, the way `KeptScreens` keeps tabs.
3. In Add cards, one +/− recomposes every visible tile, because all tiles read one counts map. Give each card its own count state.
4. Kept deck editors recompose on every save and every navigation, because their root reads `liveDecks` and `state.current`. Narrow those reads with `derivedStateOf`.
5. Each deck box validates its deck when it composes. Compute legality, colours and cover once per list.
6. The export dialog starts a `dealIn` coroutine per row and encodes its QR code on the UI thread. Drive the stagger from one animation, and encode with `produceState` on `Dispatchers.Default`.
7. Cards already in the deck keep a real shadow while the grid scrolls. Draw a cheap glow at rest and keep the shadow for hover.
8. Turn the drag-source `composed {}` modifiers into nodes.
