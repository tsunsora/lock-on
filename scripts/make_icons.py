"""Draws the Lock On mark (a bold targeting reticle with a red crest on a yellow tile) for desktop packaging.

Uses the same 108-unit geometry as LockOnMark in the shared UI and the Android launcher icon.
Run from the repo root:  python scripts/make_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
YELLOW = (255, 201, 77, 255)
NAVY = (20, 33, 61, 255)
RED = (211, 53, 43, 255)


def draw(size: int) -> Image.Image:
    scale = 4  # draw large, then downsample for smooth edges
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    k = s / 108

    def p(x, y):
        return x * k, y * k

    # Yellow tile with rounded corners.
    d.rounded_rectangle([0, 0, s - 1, s - 1], radius=24 * k, fill=YELLOW)

    w = round(8 * k)
    r = 26 * k
    cx, cy = p(54, 54)
    d.ellipse([cx - r - w / 2, cy - r - w / 2, cx + r + w / 2, cy + r + w / 2], outline=NAVY, width=w)
    for a, b in [((54, 14), (54, 34)), ((54, 74), (54, 94)), ((14, 54), (34, 54)), ((74, 54), (94, 54))]:
        d.line([p(*a), p(*b)], fill=NAVY, width=w)
        for end in (a, b):
            ex, ey = p(*end)
            d.ellipse([ex - w / 2, ey - w / 2, ex + w / 2, ey + w / 2], fill=NAVY)
    d.polygon([p(54, 42), p(63, 54), p(54, 66), p(45, 54)], fill=RED)
    return img.resize((size, size), Image.LANCZOS)


def main():
    icons = ROOT / "desktopApp" / "icons"
    icons.mkdir(parents=True, exist_ok=True)
    big = draw(1024)
    big.resize((512, 512), Image.LANCZOS).save(icons / "lock-on.png")
    big.save(icons / "lock-on.ico", sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    big.save(icons / "lock-on.icns")
    resources = ROOT / "desktopApp" / "src" / "main" / "resources"
    resources.mkdir(parents=True, exist_ok=True)
    draw(256).save(resources / "lock-on-window.png")
    print("Wrote icons to", icons, "and", resources)


if __name__ == "__main__":
    main()
