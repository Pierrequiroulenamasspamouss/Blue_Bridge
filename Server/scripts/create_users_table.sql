-- Script pour créer la table users avec support FCM
-- Exécutez ce script dans votre base de données MySQL

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    firstName VARCHAR(100) NOT NULL,
    lastName VARCHAR(100) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    fcmToken TEXT,
    createdAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_username (username),
    INDEX idx_fcm_token (fcmToken(255))
);

-- Insérer des utilisateurs de test
INSERT INTO users (id, firstName, lastName, username, email, password, fcmToken) VALUES
('bba26029-1c2f-4bae-9359-4e4e3d327fee', 'Test', 'User', 'testuser', 'test@example.com', 'hashed_password', 'd9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg'),
('test_receiver', 'Receiver', 'User', 'receiver', 'receiver@example.com', 'hashed_password', 'd9MrrVpSRL6ou_-Oq6FmW_:APA91bFnnfuJ25QyYuhDXYFsoI9iv-6nI3x7pXdqI0rrvdRKA74vtsUw5gyVy85uvOg2qggEq770K-F_WhqMEy_fd_HevEdLzTjGaYOZRRfj3Q75gyoOzqg')
ON DUPLICATE KEY UPDATE
    firstName = VALUES(firstName),
    lastName = VALUES(lastName),
    fcmToken = VALUES(fcmToken),
    updatedAt = CURRENT_TIMESTAMP; 