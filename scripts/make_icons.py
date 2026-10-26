"""Draws the Lock On mark (V-fin over a targeting reticle with a red crest) for desktop packaging.

Uses the same 108-unit geometry as LockOnMark in the shared UI and the Android launcher icon.
Run from the repo root:  python scripts/make_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
NAVY = (13, 19, 32, 255)
YELLOW = (255, 201, 77, 255)
WHITE = (233, 236, 242, 255)
RED = (211, 53, 43, 255)


def draw(size: int) -> Image.Image:
    scale = 4  # draw large, then downsample for smooth edges
    s = size * scale
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # Navy tile with armour-plate cut corners (top-left and bottom-right).
    cut = s * 0.1
    d.polygon([(cut, 0), (s, 0), (s, s - cut), (s - cut, s), (0, s), (0, cut)], fill=NAVY)

    # Map the 108-unit grid into the middle 76% of the tile.
    inset = s * 0.12
    k = (s - 2 * inset) / 108

    def p(x, y):
        return inset + x * k, inset + (y - 4) * k

    d.polygon([p(54, 52), p(20, 18), p(32, 18), p(54, 40), p(76, 18), p(88, 18)], fill=YELLOW)
    r = 20 * k
    cx, cy = p(54, 62)
    w = max(1, round(6 * k))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=WHITE, width=w)
    for a, b in [((54, 34), (54, 46)), ((54, 78), (54, 92)), ((24, 62), (38, 62)), ((70, 62), (84, 62))]:
        d.line([p(*a), p(*b)], fill=WHITE, width=w)
        for end in (a, b):
            ex, ey = p(*end)
            d.ellipse([ex - w / 2, ey - w / 2, ex + w / 2, ey + w / 2], fill=WHITE)
    d.polygon([p(54, 54), p(60, 62), p(54, 70), p(48, 62)], fill=RED)
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
