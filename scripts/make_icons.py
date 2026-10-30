"""Draws the Lock On app icon for desktop packaging as a flat tile: the Lock On mark (a targeting
reticle with a center dot) in white, on a tile of bright navy with a clear diagonal gradient,
lighter at the top right. No glows or shading, to match the Android launcher icon.

Uses the same 108-unit geometry as LockOnMark in the shared UI and the Android launcher icon.
Run from the repo root:  python scripts/make_icons.py
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
# The tile fades from TILE_TOP to TILE_BOTTOM; #34478C, close to their midpoint, is ic_launcher_background
# (the splash screen's solid colour).
TILE_TOP = (76, 102, 192)  # #4C66C0
TILE_BOTTOM = (31, 44, 94)  # #1F2C5E
MARK = (255, 255, 255, 255)
ARMS = [((54, 14), (54, 34)), ((54, 74), (54, 94)), ((14, 54), (34, 54)), ((74, 54), (94, 54))]


def reticle(d: ImageDraw.ImageDraw, p, k, width, fill):
    w = round(width * k)
    cx, cy = p(54, 54)
    r = 26 * k
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
    # Lighter at the top right, darker at the bottom left.
    fade = Image.new("L", (s, s))
    fade.putdata([round(255 * ((s - 1 - x) + y) / (2 * (s - 1))) for y in range(s) for x in range(s)])
    tile = Image.composite(Image.new("RGBA", (s, s), TILE_BOTTOM + (255,)), Image.new("RGBA", (s, s), TILE_TOP + (255,)), fade)
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

    reticle(d, p, k, 8, MARK)
    cx, cy = p(54, 54)
    r = 8 * k
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
