"""Only disposable own PHP/MySQL fixtures; no live domain, account or Spotify data."""
import datetime, json, os, subprocess, urllib.request, urllib.error, unittest
BASE='http://127.0.0.1:8080/api/index.php?route='
class AccountAPI(unittest.TestCase):
 def call(self,route,body=None,token=None):
  headers={'Content-Type':'application/json'}
  if token: headers['Authorization']='Bearer '+token
  req=urllib.request.Request(BASE+route,data=json.dumps(body or {}).encode(),headers=headers,method='POST')
  try:
   with urllib.request.urlopen(req,timeout=20) as r:return r.status,json.load(r)
  except urllib.error.HTTPError as e:return e.code,json.load(e)
 def admin(self,*args):subprocess.run(['php','server/bin/admin.php',*args],check=True,stdout=subprocess.DEVNULL)
 def sql(self,sql):
  script="require 'server/src/bootstrap.php';$r=query($argv[1]);echo json_encode($r->fetchAll());"
  return json.loads(subprocess.check_output(['php','-r',script,sql],text=True))
 def test_full_account_lifecycle_and_authority(self):
  password='Own fixture password 2026!'
  status,result=self.call('register',{'username':'Own_Fixture','password':password,'premium_until':9999999999,'plan':'premium'})
  self.assertEqual(201,status);token=result['token'];self.assertEqual('free',result['account']['plan']);self.assertEqual('own_fixture',result['account']['username'])
  self.assertEqual(401,self.call('session')[0]);self.assertEqual(401,self.call('session',token='0'*64)[0]);self.assertEqual(200,self.call('session',token=token)[0])
  rows=self.sql('SELECT password_hash FROM users WHERE username="own_fixture"');self.assertNotEqual(password,rows[0]['password_hash']);self.assertTrue(rows[0]['password_hash'].startswith('$argon2id$'))
  rows=self.sql('SELECT token_hash FROM sessions');self.assertTrue(all(r['token_hash']!=token for r in rows))
  self.assertEqual(401,self.call('login',{'username':'own_fixture','password':'wrong fixture password'})[0])
  self.assertEqual(422,self.call('register',{'username':'bad name','password':password})[0]);self.assertEqual(422,self.call('register',{'username':'fixture2','password':'short'})[0])
  self.assertEqual(409,self.call('register',{'username':'own_fixture','password':password})[0])
  tomorrow=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=2)).strftime('%Y-%m-%d');self.admin('premium','own_fixture',tomorrow)
  self.assertEqual('premium',self.call('session',token=token)[1]['account']['plan']);self.admin('free','own_fixture');self.assertEqual('free',self.call('session',token=token)[1]['account']['plan'])
  status,result=self.call('login',{'username':'OWN_FIXTURE','password':password});self.assertEqual(200,status);second=result['token']
  self.assertEqual(200,self.call('logout',token=token)[0]);self.assertEqual(401,self.call('session',token=token)[0]);self.assertEqual(200,self.call('session',token=second)[0])
  status,result=self.call('change-password',{'password':password,'new_password':'New fixture password 2026!'},second);self.assertEqual(200,status);third=result['token'];self.assertEqual(401,self.call('session',token=second)[0]);self.assertEqual(200,self.call('logout-all',token=third)[0]);self.assertEqual(401,self.call('session',token=third)[0])
  self.assertEqual(404,self.call('upgrade',{'plan':'premium'})[0]);self.assertEqual(401,self.call('session',token="x'*64")[0])
  for _ in range(15):last=self.call('login',{'username':'unknown_fixture','password':password})[0]
  self.assertEqual(429,last);self.admin('cleanup')
if __name__=='__main__':unittest.main()
