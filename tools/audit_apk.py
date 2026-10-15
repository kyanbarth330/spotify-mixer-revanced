#!/usr/bin/env python3
"""Read-only reproducible inventory; report metadata, never proprietary code."""
import argparse, hashlib, json, re, struct, zipfile, zlib
from pathlib import Path


def audit(path):
    with zipfile.ZipFile(path) as z:
        names=z.namelist()
        dex=[]
        for name in names:
            if not re.fullmatch(r'classes(?:\d+)?\.dex',name):continue
            b=z.read(name)
            if b[:4]!=b'dex\n':raise ValueError(f'{name}: invalid DEX magic')
            dex.append(dict(name=name,version=b[4:7].decode(),bytes=len(b),
                methods=struct.unpack_from('<I',b,0x58)[0],classes=struct.unpack_from('<I',b,0x60)[0],
                checksum_ok=struct.unpack_from('<I',b,8)[0]==zlib.adler32(b[12:]),
                signature_ok=b[12:32]==hashlib.sha1(b[32:]).digest()))
        native=[]
        for n in names:
            if not n.startswith('lib/') or not n.endswith('.so'):continue
            b=z.read(n);is64=b[4]==2
            if b[:4]!=b'\x7fELF' or b[5]!=1:raise ValueError(f'{n}: unsupported ELF')
            phoff=struct.unpack_from('<Q' if is64 else '<I',b,32 if is64 else 28)[0]
            ents,count=struct.unpack_from('<HH',b,54 if is64 else 42)
            align=[]
            for i in range(count):
                off=phoff+i*ents
                if struct.unpack_from('<I',b,off)[0]==1:
                    align.append(struct.unpack_from('<Q' if is64 else '<I',b,off+(48 if is64 else 28))[0])
            native.append(dict(name=n,bytes=len(b),load_alignment=sorted(set(align))))
        return dict(sha256=hashlib.sha256(Path(path).read_bytes()).hexdigest(),bytes=Path(path).stat().st_size,
            entries=len(names),duplicate_entries=len(names)-len(set(names)),zip_crc_failure=z.testzip(),
            has_manifest='AndroidManifest.xml' in names,has_resources='resources.arsc' in names,
            dex=dex,native_libraries=native)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('apk');a=p.parse_args()
    print(json.dumps(audit(a.apk),indent=2))
