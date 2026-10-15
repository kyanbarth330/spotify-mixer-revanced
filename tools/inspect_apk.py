#!/usr/bin/env python3
"""Inspect an APK locally without extracting or publishing proprietary code."""
import argparse, json, re, zipfile
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument('apk', type=Path)
a = p.parse_args()
with zipfile.ZipFile(a.apk) as z:
    names = z.namelist()
    dex = sorted(n for n in names if re.fullmatch(r'classes(?:\d+)?\.dex', n))
    relevant = sorted(n for n in names if 'settings' in n.lower() and n.startswith('res/'))
    result = {'apk_filename': a.apk.name, 'entries': len(names), 'dex_files': dex,
              'settings_resources': relevant, 'has_manifest': 'AndroidManifest.xml' in names}
    print(json.dumps(result, indent=2))
