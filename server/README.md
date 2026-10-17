# SpotEnhancer account service — PHP + MySQL

This is original account/membership code. It is **not deployed** automatically.
Requires PHP 8.2+ with PDO MySQL and Argon2id, MySQL 8/MariaDB with InnoDB,
and HTTPS. No frameworks, Composer, SMTP or Spotify credentials are needed.
Username/password accounts collect no email; email recovery is not implemented.

## Install on spotenhancer.sourcemod.lol

1. Put this directory on your server outside other users' web roots. Set this
   subdomain's document root to **server/public**. Never expose `src`, `bin`,
   `schema.sql` or the private config through a public directory.
2. Create a database and a dedicated database user. Import `schema.sql` through
   phpMyAdmin or the MySQL CLI. Runtime needs SELECT/INSERT/UPDATE/DELETE on these
   tables; schema creation can use a separate administrator account.
3. Copy `config.example.php` to `config.php` **outside public**. Fill in the
   database details. Generate `rate_secret` on the server using
   `php -r 'echo bin2hex(random_bytes(32)), PHP_EOL;'`. Restrict file permissions
   to the hosting user. Do not upload this file to GitHub or chat.
4. Enable a valid HTTPS certificate. Endpoint:
   `https://spotenhancer.sourcemod.lol/api/index.php?route=register`.
   Apache `.htaccess` forwards Authorization to PHP. If your host disallows
   `Options`, remove that directive and disable directory indexes in its panel.
   For nginx/PHP-FPM, pass `HTTP_AUTHORIZATION` from `$http_authorization`.
   A reverse proxy must correctly mark the origin HTTPS through trusted server
   configuration; untrusted forwarded headers are deliberately not accepted.
5. Test with a **new disposable account**, not your Spotify password. All calls
   are POST JSON. Register/login: `{"username":"example","password":"..."}`.
   The returned 64-character token is sent only as `Authorization: Bearer ...`.
   Session/logout/logout-all use `{}`. Never put tokens/passwords in a URL.
6. Schedule `php bin/admin.php cleanup` daily using your hosting cron panel.
   Back up the database/config securely; no user passwords or sessions belong
   in application/access logs. Turn `display_errors` off in production.

## Membership administration

No public upgrade API or fake payment/checkout is provided. New accounts are Free.
Only a trusted operator with server/database access assigns Premium:

```
php bin/admin.php premium username 2027-01-01
php bin/admin.php free username
php bin/admin.php revoke username
```

Expiry is UTC at the start of the date. Without SSH, use phpMyAdmin with the
same `users.premium_until` UNIX-seconds field. Do not add an unauthenticated
web admin script. The app refreshes at most every five minutes and starts Free
on each process start until verified. Expiry/network errors fail closed for
Premium; Free tools and diagnostics/account remain available. No purchases
or automatic renewals are implemented.

Six of 32 product features are Free (18.75%): Playlist Mixer, Sleep Timer,
Playlist Check, Clean Share, Quick Search and Track Details. The other 26 are
Premium. Accounts and Diagnostics are support surfaces outside the 32 count.
This membership is unrelated to Spotify Premium, account download rights,
DRM, ads or Spotify restrictions.

## Security and limitations

Passwords use Argon2id; sessions are random 256-bit opaque tokens, only SHA-256
hashes are stored server-side, with seven-day expiry. Username/IP HMAC bucket
rate limits protect credentials/registration and requests. HTTPS only, no cookies
or CORS, bounded JSON, prepared PDO statements, generic service/login errors,
password-change revocation and operator revocation. Username availability is
not secret. IP buckets trust REMOTE_ADDR, not client-supplied forwarded headers;
shared proxies/NATs share those limits. Rate-secret rotation resets buckets.

Android stores only a Keystore AES-GCM encrypted token in separate preferences,
not in settings profiles. Membership is held in memory for at most five minutes,
bounded by server membership/session expiry. Revocation takes effect on refresh;
this is client feature gating, **not tamper-proof DRM** against somebody modifying
the APK. Offline local tools cannot be made cryptographically unmodifiable by
this service. Server outages do not stop an already submitted Spotify playback
request. Sign-out is local first; remote revocation requires network delivery.

`change-password` takes old `password` and `new_password`, revokes old sessions
and returns a replacement token. No password-reset email or web administration
UI is included. Operators need an independent, authenticated support process.

Own CI starts PHP on loopback against a disposable MySQL service. HTTP is allowed
only for PHP's CLI development server on loopback with SE_TEST_HTTP=1. Production
FPM/Apache cannot use this exception. The test never contacts the live domain or
uses a Spotify binary/account. The Android fixtures inject test states through
reflection in androidTest sources only; they are not evidence of live login.
