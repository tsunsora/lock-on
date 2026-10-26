"""Converts a card spreadsheet (CSV) into a Lock On catalog file.

Columns are the card fields from docs/CATALOG_FORMAT.md. List columns (colors, traits, zones)
use | between values. Empty cells are left out.

    python scripts/catalog_from_csv.py cards.csv catalog.json --id my-gcg-en --version 3 --name "My catalog"
"""
import argparse
import csv
import json
import sys

LIST_FIELDS = {"colors", "traits", "zones"}
INT_FIELDS = {"level", "cost", "ap", "hp"}
CARD_FIELDS = ["cardNumber", "name", "type", "colors", "level", "cost", "ap", "hp", "text", "correctedText",
               "traits", "zones", "link", "setCode", "rarity"]
TYPES = {"UNIT", "PILOT", "COMMAND", "BASE", "RESOURCE"}
COLORS = {"BLUE", "GREEN", "RED", "WHITE", "PURPLE"}


def read_rows(path):
    with open(path, newline="", encoding="utf-8-sig") as f:
        return list(csv.DictReader(f))


def card_from_row(row, line):
    card = {}
    for field in CARD_FIELDS:
        value = (row.get(field) or "").strip()
        if field in LIST_FIELDS:
            card[field] = [v.strip() for v in value.split("|") if v.strip()]
        elif field in INT_FIELDS:
            if value:
                try:
                    card[field] = int(value)
                except ValueError:
                    sys.exit(f"Line {line}: {field} must be a whole number, got {value!r}.")
        elif value:
            card[field] = value
    card["type"] = card.get("type", "").upper()
    card["colors"] = [c.upper() for c in card.get("colors", [])]
    if not card.get("cardNumber") or not card.get("name"):
        sys.exit(f"Line {line}: cardNumber and name are required.")
    if card["type"] not in TYPES:
        sys.exit(f"Line {line}: type must be one of {', '.join(sorted(TYPES))}.")
    bad = [c for c in card["colors"] if c not in COLORS]
    if bad:
        sys.exit(f"Line {line}: unknown colors {bad}.")
    card.setdefault("setCode", card["cardNumber"].split("-")[0])
    return card


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("cards_csv")
    parser.add_argument("output_json")
    parser.add_argument("--id", required=True, help="Stable catalog ID; keep it the same across versions.")
    parser.add_argument("--version", required=True, type=int, help="Raise this on every change.")
    parser.add_argument("--name", required=True)
    parser.add_argument("--source", default="", help="Credit and terms for the data.")
    parser.add_argument("--printings", help="CSV with printingId, cardNumber, setCode, rarity, language, label, isAlternateArt, artworkUrl.")
    parser.add_argument("--rulesets", help="JSON file shaped like {\"rulesets\": [...]}.")
    args = parser.parse_args()

    cards = [card_from_row(row, i + 2) for i, row in enumerate(read_rows(args.cards_csv))]
    numbers = [c["cardNumber"] for c in cards]
    duplicates = sorted({n for n in numbers if numbers.count(n) > 1})
    if duplicates:
        sys.exit(f"Card numbers appear more than once: {', '.join(duplicates[:10])}")

    catalog = {
        "format": "lock-on-catalog",
        "formatVersion": 1,
        "catalogId": args.id,
        "version": args.version,
        "name": args.name,
        "source": args.source,
        "cards": cards,
        "printings": [],
    }
    if args.printings:
        for row in read_rows(args.printings):
            printing = {k: v.strip() for k, v in row.items() if v and v.strip()}
            printing["isAlternateArt"] = printing.get("isAlternateArt", "").lower() in {"1", "true", "yes"}
            catalog["printings"].append(printing)
    if args.rulesets:
        with open(args.rulesets, encoding="utf-8") as f:
            catalog["rulesets"] = json.load(f)["rulesets"]

    with open(args.output_json, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(cards)} cards and {len(catalog['printings'])} printings to {args.output_json}")


if __name__ == "__main__":
    main()
