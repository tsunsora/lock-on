# Lock On UX rules

Every screen and change follows these rules. If a change breaks one, either fix the change or change the rule here first, with the reason.

Sources and what we took from each are in [UX research](#research-behind-the-rules) at the end.

## 1. Navigation and structure

- **N1. Four destinations, no more.** The top level is **Decks**, **Cards**, **Sync** and **Settings**. Sync holds pairing and devices; Settings holds the device's own preferences, the card database and app updates. A new feature goes inside an existing destination unless it is used as often as those four. (Aniimo opens on a 16-icon menu and reviewers call it a nightmare to navigate.)
- **N2. Two levels deep at most.** A destination's own screen counts as level one. A deck's editor or a conflict comparison counts as level two. Anything further opens as a sheet or dialog over level two, never a third screen. (Stella Sora is praised for having few nested menus.)
- **N3. Back always works and never loses work.** System back, the on-screen back arrow and Escape on desktop all leave the current screen, and edits are already saved when they do.
- **N4. Wide windows split into panes instead of adding screens.** At 840 dp and wider, the editor has two areas: the card list and the deck. On phones the same views become tabs.
  - Width follows use. The card list gets about 60%. The deck panel gets the rest, and its tabs (**Deck**, **Analysis**, **Test**, **Matches**, **Versions**) hold everything about the deck, so nothing else takes width from building.
  - The Deck tab opens with a stats strip: the level curve, counts by type, Burst and Blockers, and the first legality problem in words, with a link to Analysis.
  - The Cards tab shows details in a side panel instead of a dialog.
  - The library lays decks out in a grid.
  - Reading screens such as Sync stay under about 980 dp wide.
  - Phones show three cards across.
  - **Space goes to cards.** The card list header is two rows: the search with its result count, Filters, Sort and the view toggle (and, in the editor when the list is at least 720 dp wide, the **Add to** switch), then colours as orb toggles with initials and types as chips. The second row wraps on wide panes. On phones it scrolls, starting with all five colours as 48 dp orbs and fading at its edge to show there is more. Every row that scrolls sideways fades at its edge the same way (`fadeEnd`), never cutting a chip off hard. Sort and Grid/List move into Filters, whose chip counts the filters set there.
  - Deck strips are 48 dp tall on wide windows and 58 dp on phones. The level curve above the deck list is a 48 dp row with a small caption.
  - On phones, a screen's title shares its row with that screen's main action. Rarely used actions, such as Import and Back up all on Decks, go in a ⋮ menu. Folder chips share one scrolling row with tag chips.
  - The test hand is always three cards across, so a phone shows the whole opening hand above Keep and Redraw.
- **N5. Floating navigation shares the top band.** Navigation floats over the hangar backdrop as detached plates instead of spanning the window (V9).
  - At 840 dp and wider, the player card floats top-left and the navigation pill top-right. Each screen puts its own header in the band between them (`ScreenHeader`, or `clearOfNav` for custom headers such as the editor's), so no row is spent on navigation alone.
  - **The band is the title bar on Windows.** There is no system caption: the sky runs to the window's top edge. Minimise, maximise/restore and close float in a small glass capsule just right of the pill, and the band keeps clear of it. Empty parts of the band (and the player card) move the window, and a double-click maximises it. Controls, fields and cards in the band still take their own clicks. Windows keeps the rounded corners, shadow, resize edges and Snap. On desktop windows narrower than 840 dp the capsule sits at the end of the screen header's row, which leaves room for it. Other systems keep their own title bar.
  - When the band is under 1000 dp, the editor header drops the cover thumbnail, the reticle's caption and the ruleset name, and uses short gauge labels. The legality stamp still says *Legal* or *Draft* in words (I4).
  - Below 840 dp, navigation is a floating pill (`PillNavBar`, after AnymeX's "Dynamic Pill" bar). It's about 88% of the screen wide, sits 12 dp above the bottom edge and is somewhat transparent, and content scrolls underneath it. Lists add `LocalNavClearance` to their bottom padding, so their last item can scroll clear of it. The selected destination expands into a teal pill with its icon and label. The others shrink to icons, which keep their labels for screen readers (I5). It holds only the necessities:
    - **Top level:** Decks, Cards, Sync and Settings.
    - **Inside a deck:** the app's pill steps aside, and back returns to it (N3). The editor floats its own sections in the same pill: **Deck**, **Add**, **Analysis**, **Test**, and **More** for Matches and Versions, so the pill never holds more than five items.
  - On wide windows, the same pill sits at the top right with every label showing.
  - The phone editor header keeps only back, the deck name (tap to rename), its colours, the Legal or Draft stamp, Export, and the two gauges.
  - On wide windows, panel tabs such as the deck panel's sit at the top of their panel.

## 2. Speed of building

- **S1. One tap per copy.** Adding or removing a copy is a single tap or click from both the card list and the deck list. Card details never sit between the person and a quantity change. (GNDM TCG Dex shipped "Quick Connect" so cards stop needing multiple taps.)
- **S2. Tap the card for details; buttons change counts.** The card body opens details, and the − and + buttons only change quantity. The two never swap places.
- **S3. Every rule limit is visible where you act.** In the card list, a card shows how many copies are in the deck, a **Max** tag at its copy limit, and an **Off-color** tag when adding it would make a third color. The person learns the rule without trying and failing.
- **S4. Nothing blocks a save.** Illegal or incomplete decks save as drafts. Rules produce messages, never disabled buttons.
- **S5. Results update as you type.** Search and filters apply immediately, with no Search button, and the result count is always on screen.

## 3. Information and feedback

- **I1. Numbers read like instruments.** Counts and limits use tabular figures as `current/limit` (`Main 42/50`), so totals don't jump around while you edit.
- **I2. Deck health in one line.** Every deck header shows its legal or draft state, its main-deck count, and its colors, with no scrolling needed.
- **I3. Explain the rule with the problem.** A legality message names the card, the count and the limit (*"Inferno Lancer has 5 copies; the limit is 4"*), never just *"Invalid deck."*
- **I4. Color is never the only signal.** Card colors always come with their name or initial, and status tags always include a word.
- **I5. Label every icon.** An icon on its own needs a text label next to it or an accessible description plus a tooltip on desktop. (A reviewer criticized Aniimo's controller menu for having "the most incomprehensible buttons for any given action.")

## 4. Calm by default

- **C1. No nagging.** No badges, banners or pop-ups announce anything the person didn't just do. Sync status appears on the Sync screen and as a small dot on its navigation item, and nowhere else. (Aniimo's reward systems "scream at you from multiple menus.")
- **C2. Undo instead of "Are you sure?"** Reversible actions such as deleting a deck or clearing a section happen immediately and offer **Undo** in a snackbar. A confirmation dialog is only for actions undo can't reverse, such as forgetting a paired device or overwriting from a backup.
- **C3. One bulk action instead of many single ones.** When something applies to many items, offer it once, such as *Clear section* or *Sync now*. (Aniimo reviewers asked for a claim-all.)
- **C4. Pick from options, don't type.** Wherever a value comes from a known list, such as a card name, a color, a tag already in use or a ruleset, offer it as a choice. Typing is the fallback.

## 5. Look and feel (sky glass, built on Material 3)

Lock On should feel like the home screen of a bright anime game, not a productivity app. The model is Stella Sora's home screen: a clear sky, frosted glass panels floating over it, pill buttons, a floating pill nav bar and soft glows. Material 3 still supplies behaviour, menus, text fields and accessibility. The deck-builder patterns come from the games players praise most: Hearthstone, Marvel Snap, Legends of Runeterra and Master Duel (see [research](#research-behind-the-rules)). The shared toolkit is in `ui/components/GameUi.kt`.

- **V1. Glass over sky.**
  - Large panels are frosted glass (`GlassPanel`, `Modifier.glass`): a translucent white fill, slightly brighter at the top, with a bright rim fading downward. The sky shows through.
  - Corners are soft: panels 22 dp, tiles 16 dp, and buttons, chips, tabs, search fields and stamps are full pills.
  - No stock Material controls show through. Buttons are `GameButton`, tabs `PillTabs`, chips `PillChip`, switches `PillSwitch`, segmented choices `PillSegmented`, and dialogs `GameDialog`. A dialog's main action is the teal pill, and Close or Cancel is a text button.
- **V2. Section headers.** A sparkle, the label in bold mixed case, a soft rule fading out, and the count in a tinted pill, such as `✦ Units ———— (32)`.
- **V2a. Type.** Headings, titles, labels, buttons and numbers use Quicksand (SIL Open Font License), a rounded face, in bold mixed case. Body text stays the platform sans for readability.
- **V3. Colour roles.**
  - **Primary (teal)** means "you can act" or "selected": primary buttons, the selected tab, chip, segment and destination, **+**, and counts in the deck.
  - **Secondary (sky blue)** is for information and charts, such as curves.
  - **Tertiary (soft gold)** is for celebration only: a full gauge, the *Locked on!* state, sparkles and the Lock On moment.
  - Errors keep Material's error role.
  - Card colours are for card identity: cost orbs, strip edges and hover glows.
  - Light mode is a clear sky. Dark mode is a soft twilight with stars, never black.
- **V4a. Cards are never cropped where they are the subject.**
  - Grid tiles, the test hand, card details and printings show the whole card at its real 63:88 shape, with high-quality filtering. Cost and AP/HP are never covered.
  - Deck strips (List view), the editor banner and deck boxes are the exception. They show a crop of a card's art as a backdrop, and the whole card is always one tap away.
  - Names aren't repeated under cards that already print them.
  - Cards without artwork get a drawn placeholder in the same shape, so grids never shift when images load.
- **V4. Card faces carry identity at a glance.** A tile always shows its cost (top-left). Without artwork it also shows its colour band, level and type initial. With artwork, the image sits over that placeholder, so a slow or broken link never leaves a blank.
- **V8. Game deck-builder patterns.**
  - **Deck cards:** the deck list shows each card small and whole, at its real shape, grouped by type. − ×count + and copy pips sit underneath. Desktop fits as many 104 dp cards as the panel holds, with 40 dp targets. Phones show 120 dp cards, three across, with 48 dp targets (R1).
  - **Deck strips** (as in Hearthstone) are the denser **List view**, a toggle in the stats strip. Each row has a glossy cost orb in the card's colour and the name over a crop of the art, fading in from the right. The side deck always uses strips, since its rows need the **To deck** action.
  - **Copy pips** (as in Runeterra): copies show as dots up to the copy limit, under the count, which is always a number too (I4). They're teal while filling, gold at the limit, and red past it.
  - **Gauges:** `Main 42/50` is a rounded bar. They're teal while filling, gold when exactly full, and red when over. Nothing is drawn on the bar itself (K3).
  - **Level curve** (as in MTG Arena): a compact curve sits at the top of the deck list.
  - **Deck boxes:** library tiles show the cover art behind the deck's name, its colour orbs, both gauges and the record.
  - **Hero banner:** the editor header is a frosted strip with the cover art fading in from the right, and the Lock On readout (*Locked on!* or *Not ready*) in words.
- **V9. Navigation floats and fits the theme** (as on Stella Sora's home screen, N5).
  - **The player card** is the brand: the Lock On mark in a teal-ringed level badge, the name, and the deck and card counts in currency-style pills. They're plain counts, never badges or prompts (C1).
  - **The pill** (N5): destinations float in a translucent capsule, and the selected one is a teal pill with its label.
  - **Window controls** (N5, desktop) use the pill's own glass and its exact height (K1), with three round 42 dp buttons the size of the pill's selected item, drawn glyphs, and a label and tooltip on each (I5). As on Windows itself, the three are the same size: being icon-only ranks them below the labelled destinations, and close shows its weight by turning red on hover, not by size. They tint on hover and sink a hair when pressed.
- **V5. Motion explains and gives feedback, quietly.** Every animation answers *what changed*, *where did I go* or *what can I touch*, and none of it competes with the cards. Short distances, small scales, no bounce, overshoot, tilt or sparkle. Timing lives in `Motion.kt`.
  - **Timing:** 120 ms for small exits, 200 ms for most changes, 280 ms for larger moves, all on Material 3's emphasized curves. Items in a group start 30 ms apart.
  - **Navigation:** going deeper drifts a short way forward, coming back drifts back, and switching tabs fades through.
  - **Lists:** rows glide when cards are added, removed or regrouped.
  - **Numbers:** they roll up a short way when they grow and down when they shrink. A count that grows also nudges up in size by a few percent.
  - **Adding and removing:** the card or strip takes a faint teal tint and thin rim when a copy is added, and red when one is removed.
  - **Hover (desktop):** cards rise a few pixels and cast a soft shadow tinted with their colour. Strips ease into their art. Buttons brighten a touch. Resting on a card for a moment opens the whole card beside it, never over it.
  - **Charts:** bars grow into place.
  - **Test hand:** cards fade in and settle up a short way, and shields flip over when revealed.
  - **Touch:** tiles, cards and buttons sink a hair when pressed. Phones give a light tick on each +/−.
  - **The Lock On moment:** when a deck becomes legal, the brackets close on the reticle, a soft glint crosses the editor banner and its rim warms to gold, then fades. On phones, a confirming haptic plays. It's the one signature animation, and it plays only on the change, never on opening a legal deck.
  - **Loops:** nothing loops except progress, and progress uses the reticle spinner. The sky is static.
  - **Reduced motion:** with the system's "Remove animations" setting on, every animation becomes an instant change or a very short fade. Tilt, bumps, flashes and the shine are skipped.
- **V6. Dark and light are equals.** Both themes use the same colour roles. Contrast is at least 4.5:1 for text and 3:1 for controls and tags, including text on glass over the sky.
- **V7. Decoration is never the only signal.**
  - **The sky:** a gradient with soft colour glows, a diagonal beam of light, bokeh, and a few fixed sparkles. Twilight adds stars.
  - **Sparkles** mark headings, the selected destination and celebration.
  - **The Lock On mark:** a V-fin over a targeting reticle with a red crest. It appears in the player card's badge, on face-down shields and as the launcher icon.
  - **Yellow-and-black caution stripes** are reserved for states that need attention: a deck conflict and a failed sync. Each also has words that explain it (I4).
  - All of these are original shapes, not official logos or artwork.

## 5a. Consistency (checked on every change)

These came from a UI review on 2026-10-28, where a sparkle on full gauges, an edge bar on the selected folder, a bare back arrow and window controls shorter than the nav pill all read as mistakes. Design-audit checklists agree on the same causes: values off the scale ("magic numbers"), similar elements styled differently, and decoration the eye reads as UI ([design audit](https://skills.sh/devanshudesai/agent-skills/design-audit), [design standards](https://tessl.io/registry/skills/github/rampstackco/claude-skills/design-standards)). Windows keeps its caption buttons one size and always visible ([title bar design](https://learn.microsoft.com/en-us/WINDOWS/APPS/design/basics/titlebar-design)).

- **K1. One height per row.** Buttons, fields and icon buttons that share a row are `ControlSize` (44 dp). Capsules side by side share a height: the window controls are exactly `PillNavHeight`, like the nav pill.
- **K2. Same rank, same size.** Controls of equal rank are equal in size. Rank shows through fill (teal, glass, text) and labels, never through sizes that drift apart.
- **K3. No stray marks.** Selection is a fill, never an edge bar or underline. No rule, sparkle or glow sits where it could be read as a control or a state: no lines under banners, nothing drawn on a gauge.
- **K4. Icon-only actions have a body.** Back, Export, ⋮ and similar use `GlassIconButton`, a round glass disc, never a bare glyph. Back sits 12–16 dp from the title beside it.
- **K5. Let the layout explain.** No subtitle under a screen title (`ScreenHeader` has none), no description under a setting whose name and options already say it, and one heading per group, not a heading over a single label. Words appear for news (an update, a pending name), problems (I3) and trust (T2), not to narrate what is on screen.
- **K6. Spacing on a 4 dp grid.** Gaps are 4, 8, 12, 16 or 24 dp, and similar gaps match across screens.
- **K7. Check in pictures.** Every UI change is reviewed in `ScreenshotTest` renders, in both themes, on phone and desktop, before it ships.

## 6. Reach and input

- **R1. 48 dp touch targets** on Android, including quantity buttons, even when they look smaller.
- **R2. Keyboard first on desktop.** `Ctrl+F` focuses search, `Esc` goes back, and `+` / `−` change the focused card's count. Every action also works with a mouse.
- **R3. Thumb zone on phones.** Navigation, including the editor's sections, and primary actions (New deck, Keep / Draw, Sync now) sit in the lower half of the screen.
- **R4. Offline always.** Nothing needs a network connection except syncing, and a failed sync never blocks editing.

## 7. Trust (sync and data)

- **T1. Never silently discard an edit.** Conflicting versions are kept and compared side by side, and the person chooses.
- **T2. Show where data lives.** The Sync screen states that only decks sync and that no account or server is involved.
- **T3. Errors say what to do next.** *"Could not reach the desktop. Check that Lock On is open on it…"*, not error codes.
- **T6. Seeing is not trusting.** Devices found on the network are listed with one tap to connect, but nothing pairs until a person on the computer confirms a 6-digit number that matches the phone's. Codes from QR or typing are optional extras, never the only safeguard.
- **T5. Automatic sync is quiet.** Auto-sync only runs while the app is open. It never shows a snackbar or dialog. It reports itself only on the Sync screen, as *Live* or *Waiting for the desktop*. An unreachable desktop is normal, not an error, so it never sets the error dot. Errors are for syncs the person asked for.
- **T4. No ads and no upsell.** These are the most common complaints in Gundam deck-builder store reviews.

---

## Research behind the rules

**Inspiration games**

- **Stella Sora (Yostar).** Reviewers praise a clean, modern, intuitive UI with clear labels, few nested menus and very smooth animation ([Game8 review](https://game8.co/articles/reviews/stella-sora-review)). Its "retro-future" look mixes fantasy characters with 90s-style tech, and its menus are compared to Persona 5 ([TapTap review](https://www.taptap.io/post/10565172)). Its run-based *Potentials* system offers a choice of three and saves good builds as reusable *Records* ([Game8](https://game8.co/articles/reviews/stella-sora-review), [Oslink guide](https://www.oslink.io/blog/guide/stella-sora-beginner-guide.html)).
  - Taken into Lock On: stylised HUD details (V2, V7), shallow navigation (N2), clear labels (I5), purposeful motion (V5), and the sample hand's "draw and judge" loop.
- **Aniimo (Pawprint Studio).** One review praises a UI that is "clean and fast in design" with "clear and precise information", including showing which creature suits gliding, swimming or climbing ([GamingOnPhone](https://gamingonphone.com/reviews/aniimo-game-review/)). Others criticize a 16-icon menu, reward systems that "scream at you from multiple menus", constant interruptions, unclear button prompts, and having no claim-all ([Siliconera](https://www.siliconera.com/review-aniimo-is-a-constantly-distracting-experience/), [Lootlevelchill](https://lootlevelchill.com/reviews/aniimo-review/), [Dualshockers](https://www.dualshockers.com/aniimo-review/)).
  - Taken into Lock On: suitability tags (S3, from the gliding/swimming hints), and the anti-patterns behind N1, C1, C3 and I5.

**Game deck builders (for the HUD look, V1, V5 and V8)**

No published UI teardown of these deck builders turned up, so where no source is linked, the pattern comes from how the game behaves.

- **Hearthstone (Blizzard)** is widely called the friendliest digital card game UI, built to feel like a tactile physical box ([GDC 2015, "Hearthstone: How to Create an Immersive User Interface"](https://gdcvault.com/play/1022036/Hearthstone-How-to-Create-an), [collection manager](https://hearthstone.wiki.gg/wiki/Collection)).
  - Taken into Lock On: deck-list card strips with a cost gem and an art crop, hover lift, and deck boxes.
- **Marvel Snap (Second Dinner)** is praised for a clean mobile interface where every tap gets visual feedback ([MobileSyrup](https://mobilesyrup.com/2022/10/29/marvel-snap-mobile-game-review/), [GamersDecide](https://gamersdecide.com/articles/marvel-snap-review)).
  - Taken into Lock On: press and hover feedback, add and remove tints, and counts that nudge when they grow.
- **Legends of Runeterra (Riot)** shows copies as pips and lets region colours drive a deck's identity ([patch 0.9.1](https://invenglobal.com/articles/10550/patch-091-speeds-up-animations-introduces-jinx-and-garen-themed-boards-to-legends-of-runeterra), [deck building](https://invenglobal.com/articles/9832/guide-how-do-you-build-a-deck-in-legends-of-runeterra)).
  - Taken into Lock On: copy pips, colour gems and colour edges, and the card inspect glow.
- **Yu-Gi-Oh! Master Duel (Konami)** is praised for fast filtering across thousands of cards ([Seafoam Gaming](https://seafoamgaming.com/2022/02/07/yu-gi-oh-master-duel-ios-xbox-series-x-switch-eshop-review/)).
  - Lock On already followed this with S5.
- **MTG Arena (Wizards)** is the counterexample, criticised for an editor where the big picture is hard to see ([Giant Bomb](https://giantbomb.com/users/7077/articles/some-thoughts-on-magic-the-gathering-arena)).
  - Taken into Lock On: its level curve beside the deck list.
- **Mecha HUDs:** *Gundam: Senjou no Kizuna II*'s cockpit HUD ([HUDS+GUIS](https://www.hudsandguis.com/home/2024/gundam-arcade)) and Hawken's grid-based HUDs with a single yellow accent ([HUDS+GUIS](https://www.hudsandguis.com/2013/11/27/hawken-mech-huds/)).
  - Tried first as a dark cockpit look and replaced on 2026-10-26: it read as heavy and didn't fit. Only the Lock On reticle and red kept for errors remain.
- **Stella Sora's home screen** (see above) became the model for the current look: sky, frosted glass, pills and a floating player card (V1, V9). The nav bar follows AnymeX's floating "Dynamic Pill" ([source](https://github.com/RyanYuuki/AnymeX/blob/main/lib/widgets/common/navbar/styles/floating_pill_navbar.dart)) (N5).

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
