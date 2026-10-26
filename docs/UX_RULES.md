# Lock On UX rules

Every screen and change follows these rules. If a change breaks one, either fix the change or change the rule here first, with the reason.

Sources and what we took from each are in [UX research](#research-behind-the-rules) at the end.

## 1. Navigation and structure

- **N1. Three destinations, no more.** The top level is **Decks**, **Cards** and **Sync**. A new feature goes inside an existing destination unless it is used as often as those three. (Aniimo opens on a 16-icon menu and reviewers call it a nightmare to navigate.)
- **N2. Two levels deep at most.** A destination's own screen counts as level one. A deck's editor or a conflict comparison counts as level two. Anything further opens as a sheet or dialog over level two, never a third screen. (Stella Sora is praised for having few nested menus.)
- **N3. Back always works and never loses work.** System back, the on-screen back arrow and Escape on desktop all leave the current screen, and edits are already saved when they do.
- **N4. Wide windows split into panes instead of adding screens.** At 840 dp and wider, the editor shows search, deck and analysis side by side. On phones the same panes become tabs in the same order.
- **N5. Top bar on desktop, bottom bar on phones.** At 840 dp and wider, navigation is a top bar with the Lock On mark, the LOCK ON wordmark and the three destinations, each with an icon, a label and a V-fin yellow underline when selected. Below that, it is a bottom bar with the same three destinations, where thumbs reach it. Screen-level tabs, such as the editor's, always sit at the top of the screen.

## 2. Speed of building

- **S1. One tap per copy.** Adding or removing a copy is a single tap or click from both the card list and the deck list. Card details never sit between the person and a quantity change. (GNDM TCG Dex shipped "Quick Connect" so cards stop needing multiple taps.)
- **S2. Tap the card for details; buttons change counts.** The card body opens details, and the − and + buttons only change quantity. The two never swap places.
- **S3. Every rule limit is visible where you act.** In the card list, a card shows how many copies are in the deck, a **Max** tag at its copy limit, and an **Off-color** tag when adding it would make a third color. The person learns the rule without trying and failing.
- **S4. Nothing blocks a save.** Illegal or incomplete decks save as drafts. Rules produce messages, never disabled buttons.
- **S5. Results update as you type.** Search and filters apply immediately, with no Search button, and the result count is always on screen.

## 3. Information and feedback

- **I1. Numbers read like instruments.** Counts and limits use tabular figures as `current/limit` (`Main 42/50`, `Resource 10/10`), so totals don't jump around while you edit.
- **I2. Deck health in one line.** Every deck header shows its legal or draft state, main and resource counts, and its colors, with no scrolling needed.
- **I3. Explain the rule with the problem.** A legality message names the card, the count and the limit (*"Inferno Lancer has 5 copies; the limit is 4"*), never just *"Invalid deck."*
- **I4. Color is never the only signal.** Card colors always come with their name or initial, and status tags always include a word.
- **I5. Label every icon.** An icon on its own needs a text label next to it or an accessible description plus a tooltip on desktop. (A reviewer criticized Aniimo's controller menu for having "the most incomprehensible buttons for any given action.")

## 4. Calm by default

- **C1. No nagging.** No badges, banners or pop-ups announce anything the person didn't just do. Sync status appears on the Sync screen and as a small dot on its navigation item, and nowhere else. (Aniimo's reward systems "scream at you from multiple menus.")
- **C2. Undo instead of "Are you sure?"** Reversible actions such as deleting a deck or clearing a section happen immediately and offer **Undo** in a snackbar. A confirmation dialog is only for actions undo can't reverse, such as forgetting a paired device or overwriting from a backup.
- **C3. One bulk action instead of many single ones.** When something applies to many items, offer it once, such as *Clear section* or *Sync now*. (Aniimo reviewers asked for a claim-all.)
- **C4. Pick from options, don't type.** Wherever a value comes from a known list, such as a card name, a color, a tag already in use or a ruleset, offer it as a choice. Typing is the fallback.

## 5. Look and feel (Material 3 with Gundam accents)

Lock On is a Material 3 app first. Components, behaviour, type scale, motion and accessibility follow Material. The Gundam identity is a layer on top: a mobile-suit colour scheme and a few decorative details, borrowing Stella Sora's bright, stylised presentation and Aniimo's "clean and fast, clear and precise" information.

- **V1. Material components, armour-plate surfaces.**
  - Buttons, chips, text fields, menus, dialogs, the bottom navigation bar and the FAB use stock Material 3 shapes and behaviour.
  - Large surfaces (cards, panels and card faces) use Material's shape theming to cut two opposite corners, like armour plates. Nothing small gets cut corners.
- **V2. HUD section headers.** Section headers are small uppercase labels with a short accent bar to their left, plus a right-aligned readout when the section has a count, such as `UNITS ··· 24`.
- **V3. Colour roles come from a mobile-suit tricolour.**
  - **Primary (V-fin yellow)** means "you can act": actions, the selected state and the FAB. Never use it for charts or plain information.
  - **Secondary (armour blue)** is for positive states and information, such as the *Legal* tag and charts.
  - **Tertiary (chest red)** is for rare emphasis. Errors keep Material's error role.
  - Card colours are only for card identity.
  - Dark mode is the cockpit (deep navy surfaces). Light mode is white armour.
- **V4. Card faces carry identity at a glance.** A tile always shows its color band, cost (top-left), level, and type initial, so a deck reads correctly even before real artwork is licensed.
- **V5. Motion explains, never decorates.** Animations last 150–250 ms and show where something went, such as a count changing or a panel opening. Nothing loops on its own. Turning on reduce-motion removes everything except fades.
- **V6. Dark and light are equals.** Both themes use the same colour roles, and contrast is at least 4.5:1 for text and 3:1 for controls and tags.
- **V7. Gundam details are decoration, used sparingly, and never the only signal.**
  - **The Lock On mark:** a V-fin over a targeting reticle with a red crest. It appears in the desktop top bar, on face-down shields and as the launcher icon.
  - **A tricolour stripe** (blue, white, red, yellow) along the inner edge of the navigation bar.
  - **Faint armour panel lines** on plates: a seam in the top margin, plus vent marks on deck tiles where the corner is empty. They never cross content.
  - **Yellow-and-black caution stripes,** reserved for states that need attention: a deck conflict and a failed sync. Each also has words that explain it (I4).
  - All of these are original shapes, not official logos or artwork.

## 6. Reach and input

- **R1. 48 dp touch targets** on Android, including quantity buttons, even when they look smaller.
- **R2. Keyboard first on desktop.** `Ctrl+F` focuses search, `Esc` goes back, and `+` / `−` change the focused card's count. Every action also works with a mouse.
- **R3. Thumb zone on phones.** Navigation and primary actions (New deck, Keep / Draw, Sync now) sit in the lower half of the screen.
- **R4. Offline always.** Nothing needs a network connection except syncing, and a failed sync never blocks editing.

## 7. Trust (sync and data)

- **T1. Never silently discard an edit.** Conflicting versions are kept and compared side by side, and the person chooses.
- **T2. Show where data lives.** The Sync screen states that only decks sync and that no account or server is involved.
- **T3. Errors say what to do next.** *"Could not reach the desktop. Check that Lock On is open on it…"*, not error codes.
- **T5. Automatic sync is quiet.** Auto-sync only runs while the app is open. It never shows a snackbar or dialog. It reports itself only on the Sync screen, as *Live* or *Waiting for the desktop*. An unreachable desktop is normal, not an error, so it never sets the error dot. Errors are for syncs the person asked for.
- **T4. No ads and no upsell.** These are the most common complaints in Gundam deck-builder store reviews.

---

## Research behind the rules

**Inspiration games**

- **Stella Sora (Yostar).** Reviewers praise a clean, modern, intuitive UI with clear labels, few nested menus and very smooth animation ([Game8 review](https://game8.co/articles/reviews/stella-sora-review)). Its "retro-future" look mixes fantasy characters with 90s-style tech, and its menus are compared to Persona 5 ([TapTap review](https://www.taptap.io/post/10565172)). Its run-based *Potentials* system offers a choice of three and saves good builds as reusable *Records* ([Game8](https://game8.co/articles/reviews/stella-sora-review), [Oslink guide](https://www.oslink.io/blog/guide/stella-sora-beginner-guide.html)).
  - Taken into Lock On: stylised HUD details (V2, V7), shallow navigation (N2), clear labels (I5), purposeful motion (V5), and the sample hand's "draw and judge" loop.
- **Aniimo (Pawprint Studio).** One review praises a UI that is "clean and fast in design" with "clear and precise information", including showing which creature suits gliding, swimming or climbing ([GamingOnPhone](https://gamingonphone.com/reviews/aniimo-game-review/)). Others criticize a 16-icon menu, reward systems that "scream at you from multiple menus", constant interruptions, unclear button prompts, and having no claim-all ([Siliconera](https://www.siliconera.com/review-aniimo-is-a-constantly-distracting-experience/), [Lootlevelchill](https://lootlevelchill.com/reviews/aniimo-review/), [Dualshockers](https://www.dualshockers.com/aniimo-review/)).
  - Taken into Lock On: suitability tags (S3, from the gliding/swimming hints), and the anti-patterns behind N1, C1, C3 and I5.

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
