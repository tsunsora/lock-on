# Lock On catalog format

A catalog is one UTF-8 JSON file holding every card, its printings and, optionally, the deck rules. Each device installs its own copy. Catalogs never sync between devices; only decks do.

Most people won't need this file format. **Cards → Catalog → Download the latest card database** fetches the official card list from gcg-api and converts it on the device. The format below is for other sources.

There are two ways to install a catalog file, both from **Cards → Catalog**:

- **Import file:** pick a `.json` file.
- **Update from a link:** paste an `https://` link. Lock On remembers it, so **Check for update** downloads it again later.

Every install shows a preview first. It gives the version, the card count, any problems, and which cards used in your decks the new catalog lacks. Decks are never changed. Cards a catalog doesn't have stay in decks and show as unknown.

> Check that you're allowed to use and share the card text and artwork you put in a catalog. Bandai doesn't publish an official data feed.

## Example

```json
{
  "format": "lock-on-catalog",
  "formatVersion": 1,
  "catalogId": "my-gcg-en",
  "version": 3,
  "name": "My Gundam Card Game catalog (EN)",
  "source": "Where the data came from, and its terms.",
  "cards": [
    {
      "cardNumber": "GD01-001",
      "name": "Example Unit",
      "type": "UNIT",
      "colors": ["BLUE"],
      "level": 3, "cost": 2, "ap": 3, "hp": 3,
      "text": "【Deploy】 Draw 1.",
      "correctedText": null,
      "traits": ["Earth Federation"],
      "zones": ["Space", "Earth"],
      "link": "Pilot with trait (Earth Federation)",
      "setCode": "GD01",
      "rarity": "R"
    }
  ],
  "printings": [
    { "printingId": "GD01-001", "cardNumber": "GD01-001", "setCode": "GD01", "rarity": "R" },
    { "printingId": "GD01-001_p1", "cardNumber": "GD01-001", "setCode": "GD01", "rarity": "R+",
      "label": "Alternate art", "isAlternateArt": true, "artworkUrl": "https://…" }
  ],
  "rulesets": [
    { "id": "standard", "name": "Standard", "region": "EN", "effectiveDate": "2026-10-01",
      "banned": [], "restricted": {}, "bannedCombinations": [] }
  ]
}
```

## Top-level fields

| Field | Required | Meaning |
| --- | --- | --- |
| `format` | yes | Always `lock-on-catalog`. |
| `formatVersion` | yes | `1`. Lock On refuses files from a newer format it doesn't understand. |
| `catalogId` | yes | Stable ID for this catalog. Keep it the same across versions, so a new file counts as an **update** rather than a replacement. |
| `version` | yes | Whole number. Raise it on every change; **Check for update** compares it. |
| `name` | yes | Shown in the Cards tab. |
| `source` | no | Credit and terms, shown in the Catalog sheet. |
| `cards` | yes | At least one card. Card numbers must be unique. |
| `printings` | no | Artwork, rarity and language variants. Printings for card numbers not in `cards` are skipped, with a warning. A printing's `artworkUrl` (an `https://` image link) is shown in the app. The standard printing is used unless a deck picks another, and images are cached on each device. Card images are best around 630×880 (the card's 63:88 shape). |
| `rulesets` | no | If present and not empty, these replace the deck rules. Otherwise the current rules are kept. Same fields as [rulesets.json](../shared/src/commonMain/composeResources/files/rulesets/rulesets.json). |

## Card fields

| Field | Values |
| --- | --- |
| `cardNumber` | Gameplay identity, such as `GD01-001`. Copy limits count by this. |
| `name`, `text`, `setCode`, `rarity` | Text. |
| `type` | `UNIT`, `PILOT`, `COMMAND`, `BASE` or `RESOURCE`. |
| `colors` | Any of `BLUE`, `GREEN`, `RED`, `WHITE`, `PURPLE`. Empty for resources. |
| `level`, `cost`, `ap`, `hp` | Whole numbers, or leave them out. |
| `correctedText` | Errata wording, shown instead of `text` when present. |
| `traits`, `zones` | Lists of text. |
| `link` | Link condition, either a pilot's name or `Pilot with trait (X)`. Analysis uses it to find Unit–Pilot pairs. |

Unknown extra fields are ignored, so newer catalogs can add fields without breaking older apps.

## From a spreadsheet

[scripts/catalog_from_csv.py](../scripts/catalog_from_csv.py) turns a CSV into a catalog. It needs one row per card, with the column names from the card fields table. Separate list values with `|`; for example, `colors` might be `BLUE|WHITE`.

```bash
python scripts/catalog_from_csv.py cards.csv catalog.json --id my-gcg-en --version 3 --name "My catalog"
```

Add `--printings printings.csv` for printing rows, and `--rulesets rulesets.json` to include deck rules.

## Hosting for updates

Any `https://` link that serves the raw file works, such as a file in a GitHub repository or a gist (use the *raw* link), or your own site. To publish an update, upload the new file with a higher `version`, then tap **Check for update** in Lock On.
