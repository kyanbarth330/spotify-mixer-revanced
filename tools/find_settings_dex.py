#!/usr/bin/env python3
"""Find ASCII settings-related leads in APK DEX files; does not modify Spotify."""
import argparse
import json
import re
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("apk", help="Path to an APK that you own or are authorized to inspect")
args = parser.parse_args()
terms = [b"SettingsPage", b"SettingsActivity", b"settings_page", b"SettingsRow"]
result = {}
with zipfile.ZipFile(args.apk) as apk:
    for name in sorted(apk.namelist()):
        if not re.fullmatch(r"classes(?:[0-9]+)?\.dex", name):
            continue
        data = apk.read(name)
        hits = {}
        for term in terms:
            offsets = [match.start() for match in re.finditer(re.escape(term), data)]
            if offsets:
                hits[term.decode()] = offsets[:50]
        if hits:
            result[name] = hits
print(json.dumps(result, indent=2))
print("Note: offsets are search leads, NOT validated DEX class/method references.")
