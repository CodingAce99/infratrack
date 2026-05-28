-- V3: Seed default users for Sprint 7.1
-- These are demo credentials only; in production, users would be created via API.

INSERT INTO users (id, username, password_hash, role)
VALUES
    ('00000000-0000-0000-0000-000000000001',
     'admin',
     '$2a$10$Vspu69GMLgtULUK8Ab5Zge7RVH2nP/1z2qVDF1t2GWusIdrkgZ02G',
     'ADMIN'),
    ('00000000-0000-0000-0000-000000000002',
     'viewer',
     '$2a$10$ko.0Qz5zEoLqAS820B3RCOCa6q9/JC/Hd1SKaHUYtiVo4vrGYj/4W',
     'VIEWER');