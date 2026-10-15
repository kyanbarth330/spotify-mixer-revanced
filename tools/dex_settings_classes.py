#!/usr/bin/env python3
"""Parse DEX metadata to locate settings classes and their declared method names.
Uses Python stdlib; no APK binaries or proprietary code are written to disk.
"""
import argparse, json, re, struct, zipfile

def uleb(data, offset):
    value = 0
    for shift in range(0, 35, 7):
        b = data[offset]
        offset += 1
        value |= (b & 127) << shift
        if b < 128:
            return value, offset
    raise ValueError("Invalid ULEB128")

def parse(data, pattern=r"settings|preference"):
    if data[:4] != b"dex\n":
        raise ValueError("Not a DEX")
    u32 = lambda off: struct.unpack_from("<I", data, off)[0]
    s_count, s_off = u32(0x38), u32(0x3c)
    t_count, t_off = u32(0x40), u32(0x44)
    m_count, m_off = u32(0x58), u32(0x5c)
    c_count, c_off = u32(0x60), u32(0x64)
    strings = []
    for i in range(s_count):
        off = u32(s_off + 4*i)
        _, off = uleb(data, off)
        end = data.index(0, off)
        strings.append(data[off:end].decode("utf-8", "replace"))
    types = [strings[u32(t_off+4*i)] for i in range(t_count)]
    methods = {}
    for i in range(m_count):
        cls, proto, name = struct.unpack_from("<HHI", data, m_off+8*i)
        methods.setdefault(types[cls], set()).add(strings[name])
    classes = []
    for i in range(c_count):
        off = c_off+32*i
        idx, access, parent, interfaces, source, annotations, class_data, static = struct.unpack_from("<8I", data, off)
        name = types[idx]
        if re.search(pattern, name, re.I):
            classes.append({"class": name, "super": types[parent] if parent < len(types) else None,
                            "methods": sorted(methods.get(name, []))[:80]})
    return classes

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk")
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as apk:
        result = {}
        for name in sorted(apk.namelist()):
            if re.fullmatch(r"classes(?:[0-9]+)?\.dex", name):
                found = parse(apk.read(name))
                if found:
                    result[name] = found
    print(json.dumps(result, indent=2))
if __name__ == "__main__":
    main()
