<?php
declare(strict_types=1);
if(PHP_SAPI!=='cli'){http_response_code(404);exit;}
require dirname(__DIR__).'/src/bootstrap.php';
$action=$argv[1]??'';$username=strtolower($argv[2]??'');
if(!in_array($action,['premium','free','revoke','cleanup'],true)){fwrite(STDERR,"Usage: php bin/admin.php premium USER YYYY-MM-DD | free USER | revoke USER | cleanup\n");exit(2);}
if($action==='cleanup'){query('DELETE FROM sessions WHERE expires_at<?',[time()]);query('DELETE FROM rate_limits WHERE expires_at<?',[time()]);echo "Expired sessions and rate buckets removed.\n";exit;}
$u=query('SELECT id FROM users WHERE username=?',[$username])->fetch();if(!$u){fwrite(STDERR,"Unknown user\n");exit(2);}
if($action==='revoke')query('DELETE FROM sessions WHERE user_id=?',[$u['id']]);
else {$until=0;if($action==='premium'){$date=$argv[3]??'';$dt=DateTimeImmutable::createFromFormat('!Y-m-d',$date,new DateTimeZone('UTC'));if(!$dt||$dt->format('Y-m-d')!==$date||$dt->getTimestamp()<=time()){fwrite(STDERR,"Use a future UTC expiry date\n");exit(2);}$until=$dt->getTimestamp();}query('UPDATE users SET premium_until=? WHERE id=?',[$until,$u['id']]);}
echo "Account updated. Clients refresh within five minutes.\n";
