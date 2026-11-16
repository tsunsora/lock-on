"""Draws the Lock On app icon for desktop packaging as a flat tile: the Lock On mark (a bold ring
with four studs and a center dot) in white, on one flat teal tile. No gradient, glow or shading
(UX rule V7), to match the Android launcher icon, the launch screen and the in-app logo tile.

Uses the same 108-unit geometry as LockOnMark in the shared UI and the Android launcher icon.
Run from the repo root:  python scripts/make_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
# The same teal and white as ic_launcher_background / ic_launcher_foreground and AppIconTile / AppIconMark.
TILE = (8, 117, 129)  # #087581
MARK = (255, 255, 255, 255)
ARMS = [((54, 18), (54, 29)), ((54, 79), (54, 90)), ((18, 54), (29, 54)), ((79, 54), (90, 54))]


def reticle(d: ImageDraw.ImageDraw, p, k, width, fill):
    w = round(width * k)
    cx, cy = p(54, 54)
    r = 25 * k
    d.ellipse([cx - r - w / 2, cy - r - w / 2, cx + r + w / 2, cy + r + w / 2], outline=fill, width=w)
    for a, b in ARMS:
        d.line([p(*a), p(*b)], fill=fill, width=w)
        for end in (a, b):
            ex, ey = p(*end)
            d.ellipse([ex - w / 2, ey - w / 2, ex + w / 2, ey + w / 2], fill=fill)


def draw(size: int) -> Image.Image:
    scale = 4  # draw large, then downsample for smooth edges
    s = size * scale
    u = s / 108
    tile = Image.new("RGBA", (s, s), TILE + (255,))
    shape = Image.new("L", (s, s), 0)
    ImageDraw.Draw(shape).rounded_rectangle([0, 0, s - 1, s - 1], radius=24 * u, fill=255)
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    img.paste(tile, mask=shape)
    d = ImageDraw.Draw(img)

    # The mark at 95%, leaving a margin between the arm tips and the tile's edges.
    f = 0.95
    k = u * f

    def p(x, y):
        return (54 + (x - 54) * f) * u, (54 + (y - 54) * f) * u

    reticle(d, p, k, 12, MARK)
    cx, cy = p(54, 54)
    r = 10 * k
    d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=MARK)
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
