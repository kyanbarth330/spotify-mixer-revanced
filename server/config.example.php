<?php
// Copy to config.php OUTSIDE public/. Never commit real credentials.
return [
 'dsn' => 'mysql:host=127.0.0.1;dbname=spotenhancer;charset=utf8mb4',
 'db_user' => 'spotenhancer',
 'db_password' => 'REPLACE_WITH_DATABASE_PASSWORD',
 'rate_secret' => 'REPLACE_WITH_64_RANDOM_HEX_CHARACTERS',
 'registration_enabled' => true,
];
