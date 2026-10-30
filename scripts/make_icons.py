"""Draws the Lock On app icon for desktop packaging, in the app's twilight theme: the Lock On mark
(a targeting reticle with a red crest) in the twilight accent's teal, over a tile of the twilight
sky with its teal and violet glows (Sky.kt).

Uses the same 108-unit geometry as LockOnMark in the shared UI and the Android launcher icon.
Run from the repo root:  python scripts/make_icons.py
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
SKY = [(0.0, (38, 52, 106)), (0.5, (25, 33, 63)), (1.0, (17, 23, 46))]  # TwilightSky
TEAL_GLOW = (63, 198, 212)
VIOLET_GLOW = (142, 107, 232)
RING = np.array([94, 224, 230], np.float32)  # the twilight accent
RING_LIGHT = np.array([158, 236, 240], np.float32)  # the accent lightened by 0.4, as in the badge
CREST = (229, 72, 77, 255)
ARMS = [((54, 14), (54, 34)), ((54, 74), (54, 94)), ((14, 54), (34, 54)), ((74, 54), (94, 54))]


def sky(s: int) -> Image.Image:
    y, x = np.mgrid[0:s, 0:s].astype(np.float32) / s
    rgb = np.zeros((s, s, 3), np.float32)
    for (t0, c0), (t1, c1) in zip(SKY, SKY[1:]):
        m = (y >= t0) & (y <= t1)
        f = ((y - t0) / (t1 - t0))[..., None]
        rgb[m] = (np.array(c0) * (1 - f) + np.array(c1) * f)[m]
    for cx, cy, r, color, a in [(0.86, 0.02, 0.7, TEAL_GLOW, 0.32), (0.06, 0.95, 0.7, VIOLET_GLOW, 0.3)]:
        k = (a * np.clip(1 - ((x - cx) ** 2 + (y - cy) ** 2) / (r * r), 0, 1) ** 2)[..., None]
        rgb = rgb * (1 - k) + np.array(color) * k
    return Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


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
    img = sky(s)

    # The mark at 80%, so its arms keep clear of the tile's rounded corners.
    f = 0.8
    k = u * f

    def p(x, y):
        return (54 + (x - 54) * f) * u, (54 + (y - 54) * f) * u

    mask = Image.new("L", (s, s), 0)
    reticle(ImageDraw.Draw(mask), p, k, 8, 255)
    # A soft teal glow behind the reticle, then the reticle shaded by angle like the badge's ring:
    # light at 3 o'clock, full accent at 9.
    halo = Image.new("RGBA", (s, s), tuple(int(v) for v in RING) + (0,))
    halo.putalpha(mask.filter(ImageFilter.GaussianBlur(4 * u)).point(lambda a: a * 0.18))
    img = Image.alpha_composite(img, halo)
    y, x = np.mgrid[0:s, 0:s].astype(np.float32) + 0.5
    c = s / 2
    t = np.abs((np.degrees(np.arctan2(y - c, x - c)) + 360) % 360 - 180) / 180
    teal = np.zeros((s, s, 4), np.uint8)
    teal[..., :3] = (RING * (1 - t[..., None]) + RING_LIGHT * t[..., None]).astype(np.uint8)
    teal[..., 3] = np.array(mask)
    img = Image.alpha_composite(img, Image.fromarray(teal, "RGBA"))
    ImageDraw.Draw(img).polygon([p(54, 42), p(63, 54), p(54, 66), p(45, 54)], fill=CREST)

    # Round the tile's corners.
    corners = Image.new("L", (s, s), 0)
    ImageDraw.Draw(corners).rounded_rectangle([0, 0, s - 1, s - 1], radius=24 * u, fill=255)
    img.putalpha(corners)
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
