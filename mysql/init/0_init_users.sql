CREATE DATABASE IF NOT EXISTS `moto-bot`;
CREATE DATABASE IF NOT EXISTS `moto-bot_test`;

CREATE USER IF NOT EXISTS 'moto-bot'@'%' IDENTIFIED BY 'password';
GRANT ALL PRIVILEGES ON `moto-bot`.* TO 'moto-bot'@'%';
GRANT ALL PRIVILEGES ON `moto-bot_test`.* TO 'moto-bot'@'%';
FLUSH PRIVILEGES;
