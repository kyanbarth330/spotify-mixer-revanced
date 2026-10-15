#!/usr/bin/env python3
"""Local structural inventory; report metadata only, never proprietary code."""
import argparse, hashlib, json, struct, zipfile, zlib, re
from pathlib import Path
from inspect_axml_strings import strings_from_xml
p=argparse.ArgumentParser();p.add_argument('apk',type=Path);a=p.parse_args()
result={'sha256':hashlib.file_digest(a.apk.open('rb'),'sha256').hexdigest(),'size':a.apk.stat().st_size}
with zipfile.ZipFile(a.apk) as z:
    result['entries']=len(z.infolist());result['zip_crc_error']=z.testzip()
    result['duplicate_names']=len(z.namelist())-len(set(z.namelist()))
    strings=strings_from_xml(z.read('AndroidManifest.xml'))
    result['manifest_strings']={
        'version_candidate':[s for s in strings if re.fullmatch(r'9\.1\.90\.2270',s)],
        'permissions':sorted(s for s in strings if s.startswith('android.permission.')),
        'activities':sorted(s for s in strings if s.startswith('com.spotify.') and s.endswith('Activity'))}
    result['dex']=[]
    for n in z.namelist():
        if not re.fullmatch(r'classes(?:\d+)?\.dex',n):continue
        b=z.read(n)
        result['dex'].append({'name':n,'bytes':len(b),'magic':b[:8].decode('ascii'),
            'adler32_ok':struct.unpack_from('<I',b,8)[0]==zlib.adler32(b[12:]),
            'sha1_ok':b[12:32]==hashlib.sha1(b[32:]).digest(),
            'methods':struct.unpack_from('<I',b,0x58)[0],'classes':struct.unpack_from('<I',b,0x60)[0]})
    result['native_abis']=sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')})
    result['native_libraries']=sum(n.startswith('lib/') and n.endswith('.so') for n in z.namelist())
print(json.dumps(result,indent=2))
