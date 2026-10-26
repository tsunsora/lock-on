"""Turns desktopApp/build/animation-frames/<name>/frame-*.png into <name>.gif next to the screenshots.

Run after `./gradlew :desktopApp:test --tests "*AnimationPreviewTest*"`:
    python scripts/frames_to_gif.py
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
FRAMES = ROOT / "desktopApp" / "build" / "animation-frames"
OUT = ROOT / "desktopApp" / "build" / "screenshots"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for folder in sorted(p for p in FRAMES.iterdir() if p.is_dir()):
        frames = [Image.open(f).convert("RGB") for f in sorted(folder.glob("frame-*.png"))]
        if not frames:
            continue
        # Hold the last frame so the end state is readable before looping.
        durations = [33] * (len(frames) - 1) + [1200]
        target = OUT / f"{folder.name}.gif"
        frames[0].save(target, save_all=True, append_images=frames[1:], duration=durations, loop=0, optimize=True)
        print(f"{target} ({len(frames)} frames)")


if __name__ == "__main__":
    main()
