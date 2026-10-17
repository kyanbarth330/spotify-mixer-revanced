<?php
declare(strict_types=1);
function config(): array {
 static $config;
 if ($config !== null) return $config;
 $file = dirname(__DIR__).'/config.php';
 if (is_file($file)) $config = require $file;
 else $config = ['dsn'=>getenv('SE_DB_DSN'), 'db_user'=>getenv('SE_DB_USER'), 'db_password'=>getenv('SE_DB_PASSWORD'), 'rate_secret'=>getenv('SE_RATE_SECRET'), 'registration_enabled'=>getenv('SE_REGISTRATION') !== 'off'];
 if (!is_array($config) || empty($config['dsn']) || strlen((string)($config['rate_secret'] ?? '')) < 32 || str_contains((string)$config['rate_secret'],'REPLACE')) throw new RuntimeException('Server not configured');
 return $config;
}
function db(): PDO {
 static $pdo;
 if (!$pdo) {$c=config();$pdo=new PDO($c['dsn'],$c['db_user'],$c['db_password'],[PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_EMULATE_PREPARES=>false,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]);}
 return $pdo;
}
function query(string $sql,array $params=[]): PDOStatement {$q=db()->prepare($sql);$q->execute($params);return $q;}
function reply(int $status,array $body): never {http_response_code($status);echo json_encode($body,JSON_THROW_ON_ERROR);exit;}
function limit(string $scope,string $identity,int $maximum,int $seconds): void {
 $window=intdiv(time(),$seconds);$bucket=hash_hmac('sha256',$scope.'|'.$identity.'|'.$window,config()['rate_secret']);
 query('INSERT INTO rate_limits(bucket,hits,expires_at) VALUES (?,1,?) ON DUPLICATE KEY UPDATE hits=hits+1',[$bucket,($window+2)*$seconds]);
 $hits=query('SELECT hits FROM rate_limits WHERE bucket=?',[$bucket])->fetchColumn();if ((int)$hits>$maximum){header('Retry-After: '.$seconds);reply(429,['error'=>'Too many attempts. Try again later.']);}
}
function passwordHash(string $value): string {if (!defined('PASSWORD_ARGON2ID')) throw new RuntimeException('Argon2id is required');return password_hash($value,PASSWORD_ARGON2ID,['memory_cost'=>19456,'time_cost'=>2,'threads'=>1]);}
function passwordInput(array $body,string $key='password'): string {$p=$body[$key]??null;if (!is_string($p)||strlen($p)<12||strlen($p)>128||str_contains($p,"\0")) reply(422,['error'=>'Use a password of 12–128 bytes.']);return $p;}
function usernameInput(array $body): string {$u=$body['username']??null;if (!is_string($u)||!preg_match('/\A[a-zA-Z0-9_]{3,32}\z/D',$u)) reply(422,['error'=>'Use 3–32 letters, digits or underscores.']);return strtolower($u);}
function bearer(): string {$header=$_SERVER['HTTP_AUTHORIZATION']??'';if(!preg_match('/\ABearer ([a-f0-9]{64})\z/D',$header,$m)) reply(401,['error'=>'Sign in again.']);return $m[1];}
function user(): array {
 $token=bearer();$u=query('SELECT u.*,s.expires_at AS session_expires_at FROM sessions s JOIN users u ON u.id=s.user_id WHERE s.token_hash=? AND s.expires_at>?',[hash('sha256',$token),time()])->fetch();
 if (!$u) reply(401,['error'=>'Sign in again.']);return $u;
}
function profile(array $u): array {return ['username'=>$u['username'],'plan'=>(int)$u['premium_until']>time()?'premium':'free','premium_until'=>(int)$u['premium_until'],'session_expires_at'=>(int)($u['session_expires_at']??0),'checked_at'=>time()];}
function issue(array $u): array {
 $token=bin2hex(random_bytes(32));$expires=time()+7*86400;query('INSERT INTO sessions(token_hash,user_id,expires_at,created_at) VALUES (?,?,?,?)',[hash('sha256',$token),$u['id'],$expires,time()]);
 $u['session_expires_at']=$expires;return ['token'=>$token,'account'=>profile($u)];
}
