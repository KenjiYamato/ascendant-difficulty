from pathlib import Path
from PIL import Image

ICON_SIZE = 64

def process_pngs(directory: Path) -> None:
    for png in directory.glob("*.png"):
        if png.stem.endswith("@icon") or png.stem.endswith("@2x"):
            continue

        with Image.open(png) as img:
            img = img.convert("RGBA")

            # create icon
            icon = img.resize((ICON_SIZE, ICON_SIZE), Image.LANCZOS)
            icon_path = png.with_name(f"{png.stem}@icon.png")
            icon.save(icon_path, optimize=True)
            
            print(f"processed: {png.name}")

if __name__ == "__main__":
    process_pngs(Path("."))
