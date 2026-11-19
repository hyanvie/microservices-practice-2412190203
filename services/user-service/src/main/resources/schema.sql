CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(80) PRIMARY KEY
);

INSERT INTO users (id)
SELECT 'buyer-1001'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 'buyer-1001');

INSERT INTO users (id)
SELECT 'seller-1001'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 'seller-1001');

INSERT INTO users (id)
SELECT 'seller-1002'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 'seller-1002');

CREATE TABLE IF NOT EXISTS user_credentials (
    user_id VARCHAR(80) PRIMARY KEY,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT IGNORE INTO user_credentials (user_id, password_hash, role) VALUES
('buyer-1001', 'pbkdf2-sha256$210000$s1ii3qcG1lkMyleUDkcz4w==$oI1LOKhEQKBIKBUK3bKXSd6r/YKA1uPkz5hnh90HKLc=', 'BUYER'),
('seller-1001', 'pbkdf2-sha256$210000$KcLi8WWpkEUnV91Ne1F9+w==$cu8DXLzQjXEtIyyQJOMPadhYtCT5ZBWz2lKNWtRMnLM=', 'SELLER'),
('seller-1002', 'pbkdf2-sha256$210000$KcLi8WWpkEUnV91Ne1F9+w==$cu8DXLzQjXEtIyyQJOMPadhYtCT5ZBWz2lKNWtRMnLM=', 'SELLER');
