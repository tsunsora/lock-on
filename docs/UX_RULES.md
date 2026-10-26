# Lock On UX rules

Every screen and change follows these rules. If a change breaks one, either fix the change or change the rule here first, with the reason.

Sources and what we took from each are in [UX research](#research-behind-the-rules) at the end.

## 1. Navigation and structure

- **N1. Three destinations, no more.** The top level is **Decks**, **Cards** and **Sync**. A new feature goes inside an existing destination unless it is used as often as those three. (Aniimo opens on a 16-icon menu and reviewers call it a nightmare to navigate.)
- **N2. Two levels deep at most.** A destination's own screen counts as level one. A deck's editor or a conflict comparison counts as level two. Anything further opens as a sheet or dialog over level two, never a third screen. (Stella Sora is praised for having few nested menus.)
- **N3. Back always works and never loses work.** System back, the on-screen back arrow and Escape on desktop all leave the current screen, and edits are already saved when they do.
- **N4. Wide windows split into panes instead of adding screens.** At 840 dp and wider, the editor shows search, deck and analysis side by side. On phones the same panes become tabs in the same order.
- **N5. Navigation lives in one top bar on every size.** The bar holds the reticle, the wordmark on wide windows, and the three destinations, each with an icon, a label and an amber underline when selected. Phones give the three destinations equal width. Screen-level tabs, such as the editor's, sit directly under it, so "where am I" always reads top-down.

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

- **C1. No nagging.** No badges, banners or pop-ups announce anything the person didn't just do. Sync status appears on the Sync screen and as a small dot on its item in the top bar, and nowhere else. (Aniimo's reward systems "scream at you from multiple menus.")
- **C2. Undo instead of "Are you sure?"** Reversible actions such as deleting a deck or clearing a section happen immediately and offer **Undo** in a snackbar. A confirmation dialog is only for actions undo can't reverse, such as forgetting a paired device or overwriting from a backup.
- **C3. One bulk action instead of many single ones.** When something applies to many items, offer it once, such as *Clear section* or *Sync now*. (Aniimo reviewers asked for a claim-all.)
- **C4. Pick from options, don't type.** Wherever a value comes from a known list, such as a card name, a color, a tag already in use or a ruleset, offer it as a choice. Typing is the fallback.

## 5. Look and feel ("retro-future HUD")

Lock On borrows Stella Sora's bright retro-future presentation: clean anime-styled panels with a 90s targeting-computer accent. It keeps Aniimo's "clean and fast, clear and precise" information over decoration.

- **V1. Chamfered panels.** Panels, card tiles and primary buttons use cut corners, about 6–10 dp on one or two corners, rather than full rounding. The cut is the brand mark and stays consistent everywhere.
- **V2. HUD section headers.** Section headers are small uppercase labels with a short amber bar to their left, plus a right-aligned readout when the section has a count, such as `UNITS ··· 24`.
- **V3. Amber means "you can act."** The amber primary color is only for actions and the selected state. Information uses neutral text, and card colors are only for card identity.
- **V4. Card faces carry identity at a glance.** A tile always shows its color band, cost (top-left), level, and type initial, so a deck reads correctly even before real artwork is licensed.
- **V5. Motion explains, never decorates.** Animations last 150–250 ms and show where something went, such as a count changing or a panel opening. Nothing loops on its own. Turning on reduce-motion removes everything except fades.
- **V6. Dark first, light supported.** Both themes use the same color roles, and contrast is at least 4.5:1 for text and 3:1 for controls and tags.

## 6. Reach and input

- **R1. 48 dp touch targets** on Android, including quantity buttons, even when they look smaller.
- **R2. Keyboard first on desktop.** `Ctrl+F` focuses search, `Esc` goes back, and `+` / `−` change the focused card's count. Every action also works with a mouse.
- **R3. Thumb zone on phones.** Navigation is at the top (N5), so primary actions on a screen (New deck, Keep / Draw, Sync now) sit in its lower half, within thumb reach.
- **R4. Offline always.** Nothing needs a network connection except syncing, and a failed sync never blocks editing.

## 7. Trust (sync and data)

- **T1. Never silently discard an edit.** Conflicting versions are kept and compared side by side, and the person chooses.
- **T2. Show where data lives.** The Sync screen states that only decks sync and that no account or server is involved.
- **T3. Errors say what to do next.** *"Could not reach the desktop. Check that Lock On is open on it…"*, not error codes.
- **T4. No ads and no upsell.** These are the most common complaints in Gundam deck-builder store reviews.

---

## Research behind the rules

**Inspiration games**

- **Stella Sora (Yostar).** Reviewers praise a clean, modern, intuitive UI with clear labels, few nested menus and very smooth animation ([Game8 review](https://game8.co/articles/reviews/stella-sora-review)). Its "retro-future" look mixes fantasy characters with 90s-style tech, and its menus are compared to Persona 5 ([TapTap review](https://www.taptap.io/post/10565172)). Its run-based *Potentials* system offers a choice of three and saves good builds as reusable *Records* ([Game8](https://game8.co/articles/reviews/stella-sora-review), [Oslink guide](https://www.oslink.io/blog/guide/stella-sora-beginner-guide.html)).
  - Taken into Lock On: retro-future HUD styling (V1, V2), shallow navigation (N2), clear labels (I5), purposeful motion (V5), and the sample hand's "draw and judge" loop.
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
