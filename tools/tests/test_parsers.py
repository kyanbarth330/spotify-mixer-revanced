import sys, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from inspect_axml_strings import var8,var16
class StringLengthTests(unittest.TestCase):
    def test_short_and_long_utf8(self):
        self.assertEqual(var8(bytes([7]),0),(7,1))
        self.assertEqual(var8(bytes([0x81,2]),0),(258,2))
    def test_short_and_long_utf16(self):
        self.assertEqual(var16(bytes([7,0]),0),(7,2))
        self.assertEqual(var16(bytes([1,0x80,2,0]),0),(65538,4))
if __name__=='__main__': unittest.main()
