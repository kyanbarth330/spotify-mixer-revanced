#!/usr/bin/env python3
"""Verify a local patched APK's Activity registration and original class preservation."""
import sys,struct,zipfile
from inspect_axml_strings import strings_from_xml
from dex_settings_classes import parse

def verify(original,patched):
    with zipfile.ZipFile(original) as before,zipfile.ZipFile(patched) as after:
        old=strings_from_xml(before.read('AndroidManifest.xml'))
        data=after.read('AndroidManifest.xml');strings=strings_from_xml(data)
        assert strings[:len(old)]==old,'Original manifest string indexes changed'
        found=[];aliases=[];p=8
        while p<len(data):
            typ,head,size=struct.unpack_from('<HHI',data,p)
            if typ==0x102 and strings[struct.unpack_from('<I',data,p+20)[0]] in ('activity','activity-alias'):
                start,step,count=struct.unpack_from('<HHH',data,p+24);attrs={}
                for i in range(count):
                    a=p+16+start+i*step;key=struct.unpack_from('<I',data,a+4)[0];kind=data[a+15];value=struct.unpack_from('<I',data,a+16)[0]
                    attrs[strings[key]]=strings[value] if kind==3 else value
                if attrs.get('name')=='org.example.spotifymixer.runtime.SpotEnhancerActivity':found.append(attrs)
                if strings[struct.unpack_from('<I',data,p+20)[0]]=='activity-alias' and attrs.get('targetActivity')=='org.example.spotifymixer.runtime.SpotEnhancerActivity':aliases.append(attrs)
            p+=size
        assert len(found)==1 and found[0].get('exported')==0,'Missing private SpotEnhancer Activity'
        assert {a['name'] for a in aliases}=={'org.example.spotifymixer.runtime.SpotEnhancerEntry','org.example.spotifymixer.runtime.AddToMixer'},'Missing launcher/share aliases'
        assert all(a.get('exported')==1 for a in aliases),'Aliases not reachable'
        def classes(z):
            result=[]
            for name in z.namelist():
                if name.endswith('.dex'):result.extend(c['class'] for c in parse(z.read(name),pattern='.*'))
            assert len(result)==len(set(result)),'Duplicate class definitions'
            return set(result)
        oldclasses=classes(before);newclasses=classes(after)
        assert oldclasses<=newclasses,'Original classes lost'
        assert 'Lorg/example/spotifymixer/runtime/SpotEnhancerActivity;' in newclasses
        moved=parse(after.read(sorted((n for n in after.namelist() if n.endswith('.dex')),key=lambda n:1 if n=='classes.dex' else int(n[7:-4]))[-1]),pattern=r'^Lp/av10;$')
        assert len(moved)==1,'Fragment state manager was not relocated'
        print(f'PASS: private Activity, original manifest string indexes, {len(oldclasses)} original classes retained, no duplicate classes, relocated fragment hook')
if __name__=='__main__':verify(*sys.argv[1:])
