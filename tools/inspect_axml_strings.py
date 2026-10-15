#!/usr/bin/env python3
"""Inspect compiled Android XML string pools in an APK (read-only)."""
import argparse
import json
import struct
import zipfile

def u16(buf, offset):
    return struct.unpack_from("<H", buf, offset)[0]

def u32(buf, offset):
    return struct.unpack_from("<I", buf, offset)[0]

def var8(buf, offset):
    first = buf[offset]
    if first & 0x80:
        return ((first & 0x7f) << 8) | buf[offset+1], offset+2
    return first, offset+1

def var16(buf, offset):
    first = u16(buf, offset)
    if first & 0x8000:
        return ((first & 0x7fff) << 16) | u16(buf, offset+2), offset+4
    return first, offset+2

def strings_from_xml(data):
    if u16(data, 0) != 0x0003:
        raise ValueError("Not compiled Android XML")
    chunk = 8
    if u16(data, chunk) != 0x0001:
        raise ValueError("Expected string pool")
    header_size = u16(data, chunk+2)
    count = u32(data, chunk+8)
    flags = u32(data, chunk+16)
    strings_start = u32(data, chunk+20)
    utf8 = bool(flags & 0x100)
    values = []
    for index in range(count):
        offset = chunk + strings_start + u32(data, chunk+header_size+index*4)
        if utf8:
            _, offset = var8(data, offset)
            length, offset = var8(data, offset)
            value = data[offset:offset+length].decode("utf-8", "replace")
        else:
            length, offset = var16(data, offset)
            value = data[offset:offset+length*2].decode("utf-16le", "replace")
        values.append(value)
    return values

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    parser.add_argument("--resource", default="res/layout/settings_page.xml")
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as apk:
        values = strings_from_xml(apk.read(args.resource))
    print(json.dumps({"resource": args.resource, "strings": values}, indent=2))
if __name__ == "__main__":
    main()
