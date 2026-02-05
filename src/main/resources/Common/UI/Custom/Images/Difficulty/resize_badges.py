from pathlib import Path
from PIL import Image

BASE_SIZE = 128
RETINA_SIZE = 256

def process_pngs(directory: Path) -> None:
    for png in directory.glob("*.png"):
        if png.stem.endswith("@2x"):
            continue

        with Image.open(png) as img:
            img = img.convert("RGBA")

            # create @2x
            retina = img.resize((RETINA_SIZE, RETINA_SIZE), Image.LANCZOS)
            retina_path = png.with_name(f"{png.stem}@2x.png")
            retina.save(retina_path, optimize=True)

            # overwrite original as 128x128
            base = img.resize((BASE_SIZE, BASE_SIZE), Image.LANCZOS)
            base.save(png, optimize=True)

            print(f"processed: {png.name}")

if __name__ == "__main__":
    process_pngs(Path("."))
