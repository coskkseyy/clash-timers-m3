#!/usr/bin/env python3
"""Rebuilds app/src/main/assets/names_tr.json: the game's own Turkish names for every
building, troop, hero, spell, pet and trap (home village + Builder Base).

Clash keeps them in its install-time asset pack. With the phone on adb (fish):

    for p in (adb shell pm path com.supercell.clashofclans | string replace 'package:' '')
        string match -q '*asset_pack*' $p; and adb pull $p /tmp/coc-pack.apk
    end
    unzip -o -j /tmp/coc-pack.apk 'assets/localization/tr.csv' 'assets/logic/buildings.csv' \
        'assets/logic/characters.csv' 'assets/logic/heroes.csv' 'assets/logic/spells.csv' \
        'assets/logic/pets.csv' 'assets/logic/traps.csv' -d ~/Downloads/coc-loc/

then:  python3 tools/gen_names_tr.py ~/Downloads/coc-loc

The files are Supercell-compressed (optional "Sig:" + LZMA with a 4-byte size); this script
unpacks them. A data ID is the class base + the item's index (the rows with a Name), unless the
CSV has a GlobalID column. ID -> TID (logic CSV) -> Turkish (tr.csv).
"""
import csv, io, json, lzma, os, sys

BASES = {"buildings": 1000000, "characters": 4000000, "spells": 26000000,
         "heroes": 28000000, "pets": 73000000, "traps": 12000000}
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def unpack(path):
    b = open(path, "rb").read()
    if b[:4] == b"Sig:":
        b = b[68:]
    if b[:1] in (b"]", b"^"):
        b = lzma.LZMADecompressor(format=lzma.FORMAT_ALONE).decompress(b[:9] + b"\0" * 4 + b[9:])
    return b.decode("utf-8")


def main(src):
    tr = {row[0]: row[1] for row in csv.reader(io.StringIO(unpack(os.path.join(src, "tr.csv")))) if len(row) >= 2}
    english = json.load(open(os.path.join(ROOT, "app/src/main/assets/names.json")))["names"]
    out = {}
    for name, base in BASES.items():
        rows = list(csv.reader(io.StringIO(unpack(os.path.join(src, name + ".csv")))))
        head = rows[0]
        ni, ti = head.index("Name"), head.index("TID")
        gi = head.index("GlobalID") if "GlobalID" in head else None
        index = -1
        for row in rows[2:]:
            if len(row) <= ti or not row[ni]:
                continue
            index += 1
            data_id = int(row[gi]) if gi is not None and row[gi].strip().isdigit() else base + index
            if row[ti] in tr and str(data_id) in english:
                out[str(data_id)] = tr[row[ti]]
    missing = [f"{k} {v}" for k, v in english.items() if k not in out and not k.startswith(("9", "10"))]
    path = os.path.join(ROOT, "app/src/main/assets/names_tr.json")
    json.dump(out, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1, sort_keys=True)
    print(f"{len(out)} Turkish names -> {path}")
    if missing:
        print("no Turkish name:", ", ".join(missing))


if __name__ == "__main__":
    main(os.path.expanduser(sys.argv[1] if len(sys.argv) > 1 else "~/Downloads/coc-loc"))
