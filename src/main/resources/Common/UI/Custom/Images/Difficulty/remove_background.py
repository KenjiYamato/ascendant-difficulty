#!/usr/bin/env python3
from pathlib import Path
from rembg import remove
from PIL import Image


def remove_backgrounds_in_place() -> None:
    """
    Removes background from all PNG files in the current script directory
    and overwrites them in-place.
    """
    base_path = Path(__file__).resolve().parent

    for png_file in base_path.glob("*.png"):
        with Image.open(png_file).convert("RGBA") as img:
            result = remove(img)
            result.save(png_file, format="PNG")
            print(f"overwritten: {png_file.name}")


if __name__ == "__main__":
    remove_backgrounds_in_place()
