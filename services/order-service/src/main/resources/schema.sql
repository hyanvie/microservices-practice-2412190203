CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(80) PRIMARY KEY,
    item_id VARCHAR(80) NOT NULL,
    buyer_id VARCHAR(80) NOT NULL,
    seller_id VARCHAR(80) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);
