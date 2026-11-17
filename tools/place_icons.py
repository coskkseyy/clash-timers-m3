#!/usr/bin/env python3
"""Turns hand-collected images (wiki webp/png cut-outs, Clash Ninja screenshots) into widget icons.

    python3 tools/place_icons.py <folder-with-images>

Each image is trimmed to its visible pixels, padded to a square, resized to 64x64 and written to
app/src/main/assets/icons/<dataId>.webp. Screenshots (blue Clash Ninja card) get their background
removed first (tools/cut_background.py). Used for Town Hall 19, before clash-of-clans-data had it.
"""
import os, sys
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import cut_background

OUT = os.path.join(ROOT, "app/src/main/assets/icons")
SIZE = 64
# file name -> data id; screenshots are the Clash Ninja "Level N (TH19)" cards.
FILES = {
    "Town_Hall19.webp": 1000001, "Laboratory17.webp": 1000007, "Air_Defense17.webp": 1000012,
    "Builders_Hut9 (1).webp": 1000015, "Hidden_Tesla18.webp": 1000019, "Spell_Factory10.webp": 1000020,
    "Multi-Gear_Tower4_LongRange.webp": 1000079, "Multi-Archer_Tower5.webp": 1000084,
    "Ricochet_Cannon5.webp": 1000085, "Squad_Barracks12.webp": 1000106,
    "Spiky_Tower2.webp": 1000107, "Firecracker_Tower2.webp": 1000108, "Heroic_Spell_info.webp": 26000153,
}
# Troop portraits (square, full-bleed like the other troop icons): resized, not trimmed.
AVATARS = {
    "Avatar_Barch_Squad.webp": 4000195, "Avatar_Giant_Wizard_Squad.webp": 4000196,
    "Avatar_Wall_Breaker_Squad.webp": 4000197, "Avatar_Minion_Squad.webp": 4000198,
    "Avatar_Balloon_Squad.webp": 4000199, "Avatar_Golem_Witch_Squad.webp": 4000200,
    "Avatar_Hog_Rider_Squad.webp": 4000201, "Avatar_Miner_Squad.webp": 4000202,
    "Avatar_Healer_Squad.webp": 4000203, "Avatar_P.E.K.K.A_Titan_Squad.webp": 4000204,
    "Avatar_Dragons_Squad.webp": 4000205, "Avatar_Baby_Dragon_Squad.webp": 4000206,
}
SHOTS = {  # screenshot timestamp -> data id
    "011759": 1000021, "011941": 1000014, "012134": 1000071, "012155": 1000004, "012201": 1000002,
    "012208": 1000023, "012222": 1000005, "012226": 1000003, "012231": 1000024, "012242": 12000005,
    "012248": 12000008, "012253": 12000001,
}


def save(im, data_id):
    im = im.convert("RGBA")
    box = im.getchannel("A").point(lambda v: 255 if v > 8 else 0).getbbox()
    im = im.crop(box)
    side = max(im.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
    sq.resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, f"{data_id}.webp"), quality=90)
    print("wrote", data_id)


def save_portrait(im, data_id):
    im.convert("RGBA").resize((SIZE, SIZE), Image.LANCZOS).save(os.path.join(OUT, f"{data_id}.webp"), quality=90)
    print("wrote", data_id)


def main(src):
    for name, data_id in AVATARS.items():
        if os.path.exists(os.path.join(src, name)):
            save_portrait(Image.open(os.path.join(src, name)), data_id)
    for name, data_id in FILES.items():
        if os.path.exists(os.path.join(src, name)):
            save(Image.open(os.path.join(src, name)), data_id)
    for name in sorted(os.listdir(src)):
        if name.startswith("Screenshot_") and name.endswith(".png"):
            stamp = name[-10:-4]
            if stamp in SHOTS:
                save(cut_background.cut(os.path.join(src, name)), SHOTS[stamp])


if __name__ == "__main__":
    main(os.path.expanduser(sys.argv[1]))
