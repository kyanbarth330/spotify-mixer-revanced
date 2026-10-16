#!/usr/bin/env python3
"""Check the actual final installer asset, rather than a separately compiled runtime."""
import io,sys,zipfile
from dex_settings_classes import parse

def check(path):
    with zipfile.ZipFile(path) as apk:
        payload=apk.read('assets/mixer-runtime.zip')
    with zipfile.ZipFile(io.BytesIO(payload)) as runtime:
        names=runtime.namelist()
        classes=[]
        for name in names:
            classes.extend(parse(runtime.read(name), pattern=r'^Lorg/example/spotifymixer/runtime/'))
    types=[c['class'] for c in classes]
    assert len(types)==len(set(types)), 'Duplicate runtime classes'
    mixer=next((c for c in classes if c['class']=='Lorg/example/spotifymixer/runtime/Mixer;'),None)
    assert mixer and 'attach' in mixer['methods'] and 'fragmentResumed' in mixer['methods'], 'Missing runtime bridge'
    assert 'Lorg/example/spotifymixer/runtime/SpotEnhancerActivity;' in types, 'Missing SpotEnhancer screen'
    assert 'Lorg/example/spotifymixer/runtime/PlaylistUri;' in types, 'Missing playlist identity helper'
    assert 'Lorg/example/spotifymixer/runtime/Diagnostics;' in types, 'Missing in-app diagnostics'
    print(f'PASS: installer asset has {len(names)} DEX files, {len(types)} classes and Mixer.attach')
if __name__=='__main__': check(sys.argv[1])
