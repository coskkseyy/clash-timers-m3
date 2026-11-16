#!/usr/bin/env python3
"""Brings names.json and upgrade_times.json up to date from the game's own logic CSVs
(same ~/Downloads/coc-loc folder as gen_names_tr.py; see its docstring for the adb commands).
The npm `clash-of-clans-data` package lags behind new Town Halls, the game files do not.

    python3 tools/gen_game_data.py ~/Downloads/coc-loc

- upgrade_times.json: the CSV time wins wherever it is non-zero (level N = time to reach N).
  Buildings/traps hold the time on the level's own row; heroes, troops, spells and pets hold it
  on the row of the level BEFORE (time to go to the next level).
- names.json: adds buildings the package does not know yet (internal Name column, English).
Run gen_names_tr.py afterwards for the Turkish names.
"""
import csv, io, json, os, sys, importlib.util

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
spec = importlib.util.spec_from_file_location("g", os.path.join(ROOT, "tools", "gen_names_tr.py"))
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)

ASSETS = os.path.join(ROOT, "app/src/main/assets")
# file -> (base id, time columns D/H/M/S, offset: 0 = time on own row, 1 = time to next level)
LAYOUT = {
    "buildings": (1000000, ("BuildTimeD", "BuildTimeH", "BuildTimeM", "BuildTimeS"), 0),
    "traps": (12000000, ("BuildTimeD", "BuildTimeH", "BuildTimeM", None), 0),
    "heroes": (28000000, (None, "UpgradeTimeH", None, None), 1),
    "characters": (4000000, (None, "UpgradeTimeH", "UpgradeTimeM", None), 1),
    "spells": (26000000, (None, "UpgradeTimeH", None, None), 1),
    "pets": (73000000, (None, "UpgradeTimeH", "UpgradeTimeM", None), 1),
}
# Only these new names are added; everything else new in the CSVs is internal/unused.
NEW_NAMES_FROM = 1000105


def num(row, ix, col):
    if col is None or col not in ix:
        return 0
    v = row[ix[col]].strip()
    return int(v) if v.lstrip("-").isdigit() else 0


def main(src):
    times_path = os.path.join(ASSETS, "upgrade_times.json")
    names_path = os.path.join(ASSETS, "names.json")
    tj = json.load(open(times_path))
    times = tj["times"]
    nj = json.load(open(names_path))
    changed = added = 0
    for f, (base, cols, off) in LAYOUT.items():
        rows = list(csv.reader(io.StringIO(g.unpack(os.path.join(src, f + ".csv")))))
        ix = {k: i for i, k in enumerate(rows[0])}
        cur, idx, seq, name = None, -1, {}, {}
        for r in rows[2:]:
            if r[0]:
                idx += 1
                gid = r[ix["GlobalID"]] if "GlobalID" in ix else ""
                cur = int(gid) if gid.strip().isdigit() else base + idx
                name[cur] = r[0]
                seq[cur] = []
            d, h, m, s = (num(r, ix, c) for c in cols)
            seq[cur].append(((d * 24 + h) * 60 + m) * 60 + s)
        for k, secs in seq.items():
            old = times.get(str(k), {})
            # heroes etc: level 1 = 0 s normally, but Builder Base machine starts at level 1 = build time
            shift = off + (1 if off and old.get("1", 0) == 0 else 0)
            if off and old and old.get("1", 0) != 0:
                shift = 1
            for i, t in enumerate(secs):
                level = i + (1 if not off else shift)
                if not t or level < 1:
                    continue
                if old.get(str(level)) != t:
                    changed += 1 if str(level) in old else 0
                    added += 0 if str(level) in old else 1
                    times.setdefault(str(k), {})[str(level)] = t
                    old = times[str(k)]
        if f == "buildings":
            for k, n in name.items():
                if k >= NEW_NAMES_FROM and str(k) not in nj["names"]:
                    nj["names"][str(k)] = n
                    print("new building name:", k, n)
    tj["times"] = {k: {l: v for l, v in sorted(times[k].items(), key=lambda x: int(x[0]))}
                   for k in sorted(times, key=int)}
    tj["source"] = "game logic CSVs (Town Hall 19) + clash-of-clans-data@0.19.1"
    nj["names"] = dict(sorted(nj["names"].items(), key=lambda x: int(x[0])))
    nj["source"] = "clash-of-clans-data@0.19.1 + game logic CSVs (Town Hall 19)"
    json.dump(tj, open(times_path, "w"), separators=(",", ":"))
    open(times_path, "a").write("\n")
    json.dump(nj, open(names_path, "w"), indent=1)
    open(names_path, "a").write("\n")
    print(f"upgrade times: {changed} changed, {added} added")


if __name__ == "__main__":
    main(os.path.expanduser(sys.argv[1] if len(sys.argv) > 1 else "~/Downloads/coc-loc"))
