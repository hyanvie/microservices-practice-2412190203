CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(80) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    seller_id VARCHAR(80) NOT NULL,
    on_sale BOOLEAN NOT NULL
);

INSERT INTO items (id, title, price, seller_id, on_sale)
SELECT 'item-1001', 'Java 编程教材', 35.00, 'seller-1001', TRUE
WHERE NOT EXISTS (SELECT 1 FROM items WHERE id = 'item-1001');

INSERT INTO items (id, title, price, seller_id, on_sale)
SELECT 'item-1002', '无线鼠标', 28.50, 'seller-1002', TRUE
WHERE NOT EXISTS (SELECT 1 FROM items WHERE id = 'item-1002');
