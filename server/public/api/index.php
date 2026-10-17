<?php
declare(strict_types=1);
require dirname(__DIR__,2).'/src/bootstrap.php';
header('Content-Type: application/json; charset=utf-8');header('Cache-Control: no-store');header('X-Content-Type-Options: nosniff');header('Referrer-Policy: no-referrer');
try {
 $testing=PHP_SAPI==='cli-server' && getenv('SE_TEST_HTTP')==='1' && in_array($_SERVER['REMOTE_ADDR']??'', ['127.0.0.1','::1'],true);
 if (!$testing && ($_SERVER['HTTPS']??'')!=='on') reply(400,['error'=>'HTTPS is required.']);
 $method=$_SERVER['REQUEST_METHOD']??'';if ($method!=='POST') {header('Allow: POST');reply(405,['error'=>'Use POST.']);}
 if (!str_starts_with(strtolower($_SERVER['CONTENT_TYPE']??''),'application/json')) reply(415,['error'=>'Use JSON.']);
 $raw=file_get_contents('php://input',false,null,0,4097);if(strlen($raw)>4096) reply(413,['error'=>'Request too large.']);
 try{$body=json_decode($raw,false,16,JSON_THROW_ON_ERROR);}catch(JsonException $e){reply(400,['error'=>'Invalid JSON.']);}if(!($body instanceof stdClass))reply(400,['error'=>'Expected an object.']);$body=get_object_vars($body);
 $route=$_GET['route']??'';if(!in_array($route,['register','login','session','logout','logout-all','change-password'],true)) reply(404,['error'=>'Unknown endpoint.']);
 limit('request',$_SERVER['REMOTE_ADDR']??'unknown',180,60);
 if ($route==='register'||$route==='login') {
  $username=usernameInput($body);$password=passwordInput($body);
  limit('credentials-ip',$_SERVER['REMOTE_ADDR']??'unknown',30,900);limit('credentials-user',$username,12,900);
  if($route==='register') {
   if(!config()['registration_enabled']) reply(403,['error'=>'Registration is currently closed.']);limit('register',$_SERVER['REMOTE_ADDR']??'unknown',5,3600);
   try{query('INSERT INTO users(username,password_hash,created_at) VALUES (?,?,?)',[$username,passwordHash($password),time()]);}catch(PDOException $e){if($e->getCode()==='23000')reply(409,['error'=>'Choose another username.']);throw $e;}
   $u=query('SELECT * FROM users WHERE username=?',[$username])->fetch();reply(201,issue($u));
  }
  db()->beginTransaction();$u=query('SELECT * FROM users WHERE username=? FOR UPDATE',[$username])->fetch();
  // Perform an expensive password operation for unknown names as well.
  if(!$u){db()->rollBack();passwordHash($password);reply(401,['error'=>'Username or password is incorrect.']);}
  if(!password_verify($password,$u['password_hash'])){db()->rollBack();reply(401,['error'=>'Username or password is incorrect.']);}
  if(password_needs_rehash($u['password_hash'],PASSWORD_ARGON2ID,['memory_cost'=>19456,'time_cost'=>2,'threads'=>1]))query('UPDATE users SET password_hash=? WHERE id=?',[passwordHash($password),$u['id']]);
  $response=issue($u);db()->commit();reply(200,$response);
 }
 $u=user();
 if($route==='session') reply(200,['account'=>profile($u)]);
 if($route==='logout'){query('DELETE FROM sessions WHERE token_hash=?',[hash('sha256',bearer())]);reply(200,['ok'=>true]);}
 if($route==='logout-all'){query('DELETE FROM sessions WHERE user_id=?',[$u['id']]);reply(200,['ok'=>true]);}
 if($route==='change-password'){
  limit('password-change',(string)$u['id'],5,3600);$old=passwordInput($body);$new=passwordInput($body,'new_password');db()->beginTransaction();$u=query('SELECT * FROM users WHERE id=? FOR UPDATE',[$u['id']])->fetch();if(!password_verify($old,$u['password_hash'])){db()->rollBack();reply(401,['error'=>'Current password is incorrect.']);}
  $hash=passwordHash($new);try{query('UPDATE users SET password_hash=? WHERE id=?',[$hash,$u['id']]);query('DELETE FROM sessions WHERE user_id=?',[$u['id']]);db()->commit();}catch(Throwable $e){db()->rollBack();throw $e;}reply(200,issue($u));
 }
} catch(Throwable $e){$id=bin2hex(random_bytes(6));error_log('SpotEnhancer request '.$id.' failed: '.get_class($e));reply(503,['error'=>'Account service is unavailable.','request_id'=>$id]);}
