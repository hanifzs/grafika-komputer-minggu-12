"""Buat tekstur bata orisinal proyek sebagai PNG tanpa pustaka tambahan.

Jalankan `python generate_brick_texture.py` untuk membuat ulang
src/main/resources/textures/brick.png. Boleh diganti dengan gambar buatan sendiri.
"""

from pathlib import Path
import struct
import zlib


SIZE = 512
BRICK_W, BRICK_H = 128, 64
MORTAR = 4


def chunk(tag, data):
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)


def color(x, y):
    row = y // BRICK_H
    shifted_x = (x + (BRICK_W // 2 if row % 2 else 0)) % SIZE
    dx, dy = shifted_x % BRICK_W, y % BRICK_H
    if dx < MORTAR or dy < MORTAR:
        grain = ((x * 7 + y * 11) % 9) - 4
        return 188 + grain, 179 + grain, 160 + grain
    brick = shifted_x // BRICK_W
    shade = ((row * 19 + brick * 29) % 29) - 14
    grain = ((x * 37 + y * 17 + x * y * 3) % 23) - 11
    glow = 8 if dy > BRICK_H - 9 else (-8 if dy < 11 else 0)
    return (max(0, min(255, c + shade + grain + glow)) for c in (170, 82, 60))


def main():
    pixels = b"".join(b"\x00" + bytes(channel for x in range(SIZE) for channel in color(x, y))
                      for y in range(SIZE))
    png = (b"\x89PNG\r\n\x1a\n"
           + chunk(b"IHDR", struct.pack(">IIBBBBB", SIZE, SIZE, 8, 2, 0, 0, 0))
           + chunk(b"IDAT", zlib.compress(pixels, 9)) + chunk(b"IEND", b""))
    output = Path(__file__).resolve().parent / "src/main/resources/textures/brick.png"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(png)
    print(f"Dibuat: {output}")


if __name__ == "__main__":
    main()
