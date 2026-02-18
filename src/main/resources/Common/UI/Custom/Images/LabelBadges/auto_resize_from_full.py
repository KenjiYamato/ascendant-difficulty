from PIL import Image
import numpy as np
from glob import glob
import os
from collections import deque

SIZE_2X = (400, 80)
PADDING = 4
BG_TOL = 24

def scale(size, factor):
    return (int(size[0] * factor), int(size[1] * factor))

sizes = {
    "@2x": SIZE_2X,
    "": scale(SIZE_2X, 0.5),
    "@icon": scale(SIZE_2X, 0.25),
}

def alpha_trim_bbox(arr_rgba: np.ndarray):
    alpha = arr_rgba[:, :, 3]
    ys, xs = np.where(alpha > 0)
    if len(xs) == 0:
        return None
    return xs.min(), ys.min(), xs.max() + 1, ys.max() + 1

def bg_floodfill_to_transparent(arr_rgba: np.ndarray, tol: int):
    h, w = arr_rgba.shape[:2]
    corners = np.array([
        arr_rgba[0, 0, :3],
        arr_rgba[0, w-1, :3],
        arr_rgba[h-1, 0, :3],
        arr_rgba[h-1, w-1, :3],
    ], dtype=np.int16)
    bg = np.median(corners, axis=0).astype(np.int16)

    rgb = arr_rgba[:, :, :3].astype(np.int16)
    diff = np.abs(rgb - bg)
    similar = (diff[:, :, 0] <= tol) & (diff[:, :, 1] <= tol) & (diff[:, :, 2] <= tol)

    bg_mask = np.zeros((h, w), dtype=bool)
    q = deque()

    for x in range(w):
        if similar[0, x]:
            bg_mask[0, x] = True; q.append((0, x))
        if similar[h-1, x]:
            bg_mask[h-1, x] = True; q.append((h-1, x))
    for y in range(h):
        if similar[y, 0]:
            bg_mask[y, 0] = True; q.append((y, 0))
        if similar[y, w-1]:
            bg_mask[y, w-1] = True; q.append((y, w-1))

    # BFS flood-fill (4-neighborhood)
    while q:
        y, x = q.popleft()
        for ny, nx in ((y-1,x), (y+1,x), (y,x-1), (y,x+1)):
            if 0 <= ny < h and 0 <= nx < w and (not bg_mask[ny, nx]) and similar[ny, nx]:
                bg_mask[ny, nx] = True
                q.append((ny, nx))

    arr_rgba[bg_mask, 3] = 0
    return arr_rgba

def trim_with_padding(img: Image.Image, padding: int, bg_tol: int):
    rgba = img.convert("RGBA")
    arr = np.array(rgba)

    bbox = alpha_trim_bbox(arr)

    if bbox is None or bbox == (0, 0, rgba.width, rgba.height):
        arr2 = bg_floodfill_to_transparent(arr.copy(), bg_tol)
        bbox = alpha_trim_bbox(arr2)
        if bbox is None:
            return None
        arr = arr2

    left, top, right, bottom = bbox
    left = max(0, left - padding)
    top = max(0, top - padding)
    right = min(rgba.width, right + padding)
    bottom = min(rgba.height, bottom + padding)

    return Image.fromarray(arr, "RGBA").crop((left, top, right, bottom))

def resize_and_center(img: Image.Image, target_size):
    target_w, target_h = target_size
    src_w, src_h = img.size

    ratio = min(target_w / src_w, target_h / src_h)
    new_size = (max(1, int(src_w * ratio)), max(1, int(src_h * ratio)))
    resized = img.resize(new_size, Image.LANCZOS)

    canvas = Image.new("RGBA", target_size, (0, 0, 0, 0))
    ox = (target_w - new_size[0]) // 2
    oy = (target_h - new_size[1]) // 2
    canvas.paste(resized, (ox, oy), resized)
    return canvas

for path in glob("*@full.png"):
    base = path.replace("@full.png", "")

    with Image.open(path) as img:
        trimmed = trim_with_padding(img, PADDING, BG_TOL)
        if trimmed is None:
            print(f"Skipped (empty): {path}")
            continue

        for suffix, size in sizes.items():
            out_name = f"{base}{suffix}.png"
            if os.path.exists(out_name):
                print(f"Skip exists: {out_name}")
                continue

            out_img = resize_and_center(trimmed, size)
            out_img.save(out_name)
            print(f"Saved: {out_name} ({size[0]}x{size[1]})")
