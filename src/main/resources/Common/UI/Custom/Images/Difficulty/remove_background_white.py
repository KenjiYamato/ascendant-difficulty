#!/usr/bin/env python3
from __future__ import annotations

from collections import deque
from pathlib import Path

from PIL import Image


def _is_near_white(_r: int, _g: int, _b: int, _threshold: int) -> bool:
    return _r >= _threshold and _g >= _threshold and _b >= _threshold


def _build_border_connected_white_mask(_img: Image.Image, _threshold: int) -> list[list[bool]]:
    """
    Returns a 2D boolean mask where True means: pixel is near-white AND connected to border (4-neighborhood).
    """
    _rgba = _img.convert("RGBA")
    _px = _rgba.load()
    _w, _h = _rgba.size

    _vis = [[False] * _w for _ in range(_h)]
    _mask = [[False] * _w for _ in range(_h)]
    _q: deque[tuple[int, int]] = deque()

    def _try_push(_x: int, _y: int) -> None:
        if _x < 0 or _y < 0 or _x >= _w or _y >= _h:
            return
        if _vis[_y][_x]:
            return
        _r, _g, _b, _a = _px[_x, _y]
        if _a == 0:
            return
        if not _is_near_white(_r, _g, _b, _threshold):
            return
        _vis[_y][_x] = True
        _q.append((_x, _y))

    for _x in range(_w):
        _try_push(_x, 0)
        _try_push(_x, _h - 1)
    for _y in range(_h):
        _try_push(0, _y)
        _try_push(_w - 1, _y)

    while _q:
        _x, _y = _q.popleft()
        _mask[_y][_x] = True
        _try_push(_x + 1, _y)
        _try_push(_x - 1, _y)
        _try_push(_x, _y + 1)
        _try_push(_x, _y - 1)

    return _mask


def _apply_transparency(_img: Image.Image, _mask: list[list[bool]]) -> Image.Image:
    _rgba = _img.convert("RGBA")
    _px = _rgba.load()
    _w, _h = _rgba.size

    for _y in range(_h):
        _row = _mask[_y]
        for _x in range(_w):
            if _row[_x]:
                _px[_x, _y] = (0, 0, 0, 0)

    return _rgba


def remove_border_connected_white_in_place() -> None:
    """
    Removes only border-connected near-white background from all PNG files in the current script directory
    and overwrites them in-place.
    """
    _base_path = Path(__file__).resolve().parent

    # Tuning: 250..255 if background is "full white".
    # Lower if there are compression artifacts / off-white background.
    _threshold = 252

    _processed = 0
    for _png_file in sorted(_base_path.glob("*.png")):
        if _png_file.name.endswith(".tmp"):
            continue

        with Image.open(_png_file) as _img:
            _mask = _build_border_connected_white_mask(_img, _threshold=_threshold)
            _result = _apply_transparency(_img, _mask)

            _tmp = _png_file.with_suffix(".png.tmp")
            _result.save(_tmp, format="PNG")

        _tmp.replace(_png_file)
        print(f"overwritten: {_png_file.name}")
        _processed += 1

    print(f"done: {_processed} file(s)")


if __name__ == "__main__":
    remove_border_connected_white_in_place()
