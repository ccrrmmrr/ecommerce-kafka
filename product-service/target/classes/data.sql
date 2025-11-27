INSERT INTO products (sku, name, description, price, stock, created_at, updated_at) 
VALUES 
('PROD-001', 'Laptop Gaming', 'Laptop para gaming de alta performance', 1500.00, 10, NOW(), NOW()),
('PROD-002', 'Teclado Mecánico', 'Teclado mecánico RGB', 120.00, 25, NOW(), NOW()),
('PROD-003', 'Mouse Inalámbrico', 'Mouse ergonómico inalámbrico', 80.00, 30, NOW(), NOW()),
('PROD-004', 'Monitor 4K', 'Monitor 27 pulgadas 4K', 400.00, 15, NOW(), NOW())
ON CONFLICT (sku) DO NOTHING;
