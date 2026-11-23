-- Demo data for the dev profile. It started from the mobile app seed (assets/seed/*.json) and adds
-- the states the app must handle: every account status, hidden, deleted and resubmitted businesses,
-- product attributes, a blocked chat, moderation reports, every metric type and enough businesses
-- to need a second directory page.
-- Repeatable and idempotent: Flyway re-runs it when it changes and existing rows are kept.
-- Every demo account logs in with the password Demo1234 (BCrypt hash below), except the deleted ones.
-- Login outcomes: ignacia.munoz and bruno.silva are suspended (refused), diego.morales is
-- deactivated (the login reactivates it).

INSERT INTO user_account (user_id, email, password_hash, role, alias, status, consent_at, created_at, updated_at, deactivated_at, deleted_at) VALUES
    (1, 'admin1@textilcircular.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ADMINISTRATOR', 'Admin Textil', 'ACTIVE', '2026-08-01T12:00:00Z', '2026-08-01T12:00:00Z', '2026-08-01T12:00:00Z', NULL, NULL),
    (2, 'admin2@textilcircular.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ADMINISTRATOR', 'Admin Circular', 'ACTIVE', '2026-08-02T12:00:00Z', '2026-08-02T12:00:00Z', '2026-08-02T12:00:00Z', NULL, NULL),
    (3, 'valentina.mora@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-03T12:00:00Z', '2026-08-03T12:00:00Z', '2026-08-03T12:00:00Z', NULL, NULL),
    (4, 'camila.soto@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-04T12:00:00Z', '2026-08-04T12:00:00Z', '2026-08-04T12:00:00Z', NULL, NULL),
    (5, 'javiera.pinto@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-05T12:00:00Z', '2026-08-05T12:00:00Z', '2026-08-05T12:00:00Z', NULL, NULL),
    (6, 'daniela.rios@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-06T12:00:00Z', '2026-08-06T12:00:00Z', '2026-08-06T12:00:00Z', NULL, NULL),
    (7, 'fernanda.lagos@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-07T12:00:00Z', '2026-08-07T12:00:00Z', '2026-08-07T12:00:00Z', NULL, NULL),
    (8, 'martin.diaz@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Martín', 'ACTIVE', '2026-08-08T12:00:00Z', '2026-08-08T12:00:00Z', '2026-08-08T12:00:00Z', NULL, NULL),
    (9, 'sofia.reyes@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Sofi', 'ACTIVE', '2026-08-09T12:00:00Z', '2026-08-09T12:00:00Z', '2026-08-09T12:00:00Z', NULL, NULL),
    (10, 'pablo.vega@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Pablo V.', 'ACTIVE', '2026-08-10T12:00:00Z', '2026-08-10T12:00:00Z', '2026-08-10T12:00:00Z', NULL, NULL),
    (11, 'ana.castro@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Ana', 'ACTIVE', '2026-08-11T12:00:00Z', '2026-08-11T12:00:00Z', '2026-08-11T12:00:00Z', NULL, NULL),
    (12, 'lucia.herrera@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-12T12:00:00Z', '2026-08-12T12:00:00Z', '2026-08-12T12:00:00Z', NULL, NULL),
    (13, 'rocio.fuentes@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-13T12:00:00Z', '2026-08-13T12:00:00Z', '2026-08-13T12:00:00Z', NULL, NULL),
    (14, 'tomas.araya@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE', '2026-08-14T12:00:00Z', '2026-08-14T12:00:00Z', '2026-08-14T12:00:00Z', NULL, NULL),
    -- Deleted accounts look as ProfileService.delete leaves them: anonymized email, no alias, disabled password.
    (15, 'deleted-3f1c0a52-6c1e-4b7e-9a51-0d2b7f4e8c11@anonymized.invalid', '!disabled', 'ENTREPRENEUR', NULL, 'DELETED', '2026-08-15T12:00:00Z', '2026-08-15T12:00:00Z', '2026-10-20T12:00:00Z', NULL, '2026-10-20T12:00:00Z'),
    (16, 'ignacia.munoz@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'SUSPENDED', '2026-08-16T12:00:00Z', '2026-08-16T12:00:00Z', '2026-10-05T12:00:00Z', NULL, NULL),
    (17, 'diego.morales@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Diego', 'DEACTIVATED', '2026-08-17T12:00:00Z', '2026-08-17T12:00:00Z', '2026-10-10T12:00:00Z', '2026-10-10T12:00:00Z', NULL),
    (18, 'bruno.silva@example.cl', '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'CUSTOMER', 'Bruno', 'SUSPENDED', '2026-08-18T12:00:00Z', '2026-08-18T12:00:00Z', '2026-09-30T10:00:00Z', NULL, NULL),
    (19, 'deleted-8b2d4e6f-1a3c-4e5d-9f70-2c4b6d8e0a13@anonymized.invalid', '!disabled', 'CUSTOMER', NULL, 'DELETED', '2026-08-19T12:00:00Z', '2026-08-19T12:00:00Z', '2026-10-25T12:00:00Z', NULL, '2026-10-25T12:00:00Z')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO entrepreneur (user_id, age_range_id, gender_id, rut, first_name, last_name, anonymized_at) VALUES
    (3, 4, 1, '16482905-7', 'Valentina', 'Mora', NULL),
    (4, 3, 1, '17934521-8', 'Camila', 'Soto', NULL),
    (5, 5, 1, '15208736-5', 'Javiera', 'Pinto', NULL),
    (6, 3, 1, '18345092-1', 'Daniela', 'Ríos', NULL),
    (7, 4, 3, '14762318-6', 'Fernanda', 'Lagos', NULL),
    (12, 3, 1, '19283746-4', 'Lucía', 'Herrera', NULL),
    (13, 2, 1, '13572468-8', 'Rocío', 'Fuentes', NULL),
    (14, 3, 2, '17654329-9', 'Tomás', 'Araya', NULL),
    (15, 5, 4, NULL, NULL, NULL, '2026-10-20T12:00:00Z'),
    (16, 4, 1, '16098234-9', 'Ignacia', 'Muñoz', NULL)
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO business (business_id, user_id, commune_id, name, description, photo_url, status, review_round, approved_at, hidden_at, created_at, updated_at, deleted_at) VALUES
    (1, 3, 102, 'Hilos de Barrio', 'Arreglos y ajustes de ropa en Ñuñoa. Alargamos la vida de tus prendas con reparaciones visibles y bordados que cuentan una historia.', 'https://images.unsplash.com/photo-1528459801416-a9e53bbf4e17?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-03T12:00:00Z', NULL, '2026-09-01T12:00:00Z', '2026-09-03T12:00:00Z', NULL),
    (2, 4, 45, 'Retazos Circulares', 'Bolsos, cojines y chaquetas hechos con retazos de tela y ropa en desuso. Cada pieza es única y nace de la costura local de Valparaíso.', 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-05T12:00:00Z', NULL, '2026-09-02T12:00:00Z', '2026-09-05T12:00:00Z', NULL),
    (3, 5, 219, 'Ropero Compartido', 'Arriendo de ropa de fiesta y venta de ropa de segunda mano seleccionada en Concepción. Vestir bien sin comprar nuevo.', 'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-08T12:00:00Z', NULL, '2026-09-04T12:00:00Z', '2026-09-08T12:00:00Z', NULL),
    (4, 6, 252, 'Taller Telar Sur', 'Talleres de telar y bordado en Temuco para aprender a darle una segunda vida a las telas.', 'https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?auto=format&fit=crop&w=800&q=80', 'PENDING', 1, NULL, NULL, '2026-09-20T12:00:00Z', '2026-09-20T12:00:00Z', NULL),
    (5, 7, 296, 'Moda Sin Fin', 'Ropa nueva a partir de telas recicladas.', 'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=800&q=80', 'REJECTED', 1, NULL, NULL, '2026-09-10T12:00:00Z', '2026-09-10T12:00:00Z', NULL),
    (6, 12, 105, 'Costura Consciente', 'Ropa hecha en Providencia con telas recuperadas de fábricas textiles. Series cortas en tallas XS a XL.', 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-26T12:00:00Z', NULL, '2026-09-23T12:00:00Z', '2026-09-26T12:00:00Z', NULL),
    -- Paused by its owner: approved but out of the directory.
    (7, 13, 51, 'Segunda Piel Viña', 'Ropa de segunda mano revisada y lavada en Viña del Mar.', 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-28T12:00:00Z', '2026-10-15T12:00:00Z', '2026-09-24T12:00:00Z', '2026-10-15T12:00:00Z', NULL),
    -- Rejected in round 1, edited by its owner and waiting for votes in round 2.
    (8, 14, 30, 'Remiendo Norte', 'Diseño a pedido con mezclilla en desuso en La Serena. Cuéntanos tu idea y la armamos con jeans que ya no usas.', 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80', 'PENDING', 2, NULL, NULL, '2026-09-25T12:00:00Z', '2026-10-08T12:00:00Z', NULL),
    -- Soft deleted together with its owner's account.
    (9, 15, 284, 'Telas del Río', 'Accesorios hechos con telas recuperadas en Valdivia.', 'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-12T12:00:00Z', NULL, '2026-09-08T12:00:00Z', '2026-10-20T12:00:00Z', '2026-10-20T12:00:00Z'),
    -- Approved, but its owner is suspended, so v_catalog leaves it out.
    (10, 16, 12, 'Hebra Desierto', 'Reparación y modificación de ropa en Antofagasta.', 'https://images.unsplash.com/photo-1582552938357-32b906df40cb?auto=format&fit=crop&w=800&q=80', 'APPROVED', 1, '2026-09-18T12:00:00Z', NULL, '2026-09-14T12:00:00Z', '2026-09-18T12:00:00Z', NULL)
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (1, 4, 1), (1, 5, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 1);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (2, 2, 1), (2, 6, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 2);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (3, 1, 1), (3, 7, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 3);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (4, 3, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 4);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (5, 6, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 5);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (6, 6, 1), (6, 5, 2)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 6);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (7, 7, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 7);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (8, 2, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 8);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (9, 5, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 9);
INSERT INTO business_service (business_id, service_id, sort_order)
SELECT v.* FROM (VALUES (10, 4, 1)) AS v(business_id, service_id, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = 10);

INSERT INTO business_review (review_id, business_id, reviewer_id, review_round, decision, rejection_reason, comment, reviewed_at) VALUES
    (1, 1, 1, 1, 'APPROVED', NULL, NULL, '2026-09-02T12:00:00Z'),
    (2, 1, 2, 1, 'APPROVED', NULL, 'Muy buena propuesta', '2026-09-03T12:00:00Z'),
    (3, 2, 1, 1, 'APPROVED', NULL, NULL, '2026-09-04T12:00:00Z'),
    (4, 2, 2, 1, 'APPROVED', NULL, NULL, '2026-09-05T12:00:00Z'),
    (5, 3, 2, 1, 'APPROVED', NULL, NULL, '2026-09-07T12:00:00Z'),
    (6, 3, 1, 1, 'APPROVED', NULL, 'Linda ficha', '2026-09-08T12:00:00Z'),
    (7, 4, 1, 1, 'APPROVED', NULL, 'Falta que el segundo voto confirme', '2026-09-21T12:00:00Z'),
    (8, 5, 2, 1, 'REJECTED', 'UNCLEAR_DESCRIPTION', 'La descripción no explica cómo reutilizan las telas. Agrega más detalle y vuelve a enviarla.', '2026-09-11T12:00:00Z'),
    (9, 6, 1, 1, 'APPROVED', NULL, NULL, '2026-09-25T12:00:00Z'),
    (10, 6, 2, 1, 'APPROVED', NULL, NULL, '2026-09-26T12:00:00Z'),
    -- Entrepreneurs with an approved business can vote too.
    (11, 7, 1, 1, 'APPROVED', NULL, NULL, '2026-09-27T12:00:00Z'),
    (12, 7, 4, 1, 'APPROVED', NULL, 'Buena selección de prendas', '2026-09-28T12:00:00Z'),
    (13, 8, 2, 1, 'REJECTED', 'NOT_CIRCULAR', 'No queda claro de dónde sale la tela. Indica que trabajas con mezclilla en desuso.', '2026-09-30T12:00:00Z'),
    (14, 8, 3, 2, 'APPROVED', NULL, 'Ahora sí se entiende la propuesta', '2026-10-10T12:00:00Z'),
    (15, 9, 1, 1, 'APPROVED', NULL, NULL, '2026-09-11T12:00:00Z'),
    (16, 9, 2, 1, 'APPROVED', NULL, NULL, '2026-09-12T12:00:00Z'),
    (17, 10, 1, 1, 'APPROVED', NULL, NULL, '2026-09-17T12:00:00Z'),
    (18, 10, 5, 1, 'APPROVED', NULL, NULL, '2026-09-18T12:00:00Z')
ON CONFLICT (review_id) DO NOTHING;

INSERT INTO product (product_id, business_id, service_id, title, description, price, size, condition, material, offering_type, status, created_at, updated_at, deleted_at) VALUES
    (1, 1, 4, 'Reparación de jeans', 'Cambiamos cierres, cosemos rotos y reforzamos rodillas. Entrega en 5 días hábiles.', 8000, NULL, NULL, 'Mezclilla', 'REPAIR', 'ACTIVE', '2026-09-04T12:00:00Z', '2026-09-04T12:00:00Z', NULL),
    (2, 1, 5, 'Parche bordado a mano', 'Parches bordados para cubrir manchas o desgastes. Elige el diseño.', 3500, 'ONE_SIZE', 'NEW', 'Hilo de algodón', 'MADE_TO_ORDER', 'ACTIVE', '2026-09-06T12:00:00Z', '2026-09-06T12:00:00Z', NULL),
    (3, 1, 4, 'Ajuste de pantalón', 'Largo, basta o cintura. Cotizamos según la prenda.', NULL, NULL, NULL, NULL, 'REPAIR', 'ACTIVE', '2026-09-07T12:00:00Z', '2026-09-07T12:00:00Z', NULL),
    (4, 2, 2, 'Tote bag de retazos', 'Bolso de tela con asas reforzadas, hecho con retazos de tapicería.', 12000, 'ONE_SIZE', 'NEW', 'Tela de tapicería', 'MADE_TO_ORDER', 'ACTIVE', '2026-09-06T12:00:00Z', '2026-09-06T12:00:00Z', NULL),
    (5, 2, 6, 'Chaqueta patchwork', 'Chaqueta única hecha con retazos de mezclilla y lino. Talla M.', 45000, 'M', 'NEW', 'Mezclilla y lino', NULL, 'ACTIVE', '2026-09-09T12:00:00Z', '2026-09-09T12:00:00Z', NULL),
    (6, 2, 2, 'Cojín de retazos', 'Cojín de 40x40 cm. Por ahora sin stock.', 9000, 'ONE_SIZE', 'NEW', 'Retazos de algodón', 'MADE_TO_ORDER', 'HIDDEN', '2026-09-10T12:00:00Z', '2026-09-10T12:00:00Z', NULL),
    (7, 3, 1, 'Arriendo vestido de fiesta', 'Vestido largo verde, talla 38. Arriendo por 3 días.', 25000, 'M', 'GOOD', 'Satén', NULL, 'ACTIVE', '2026-09-09T12:00:00Z', '2026-09-09T12:00:00Z', NULL),
    (8, 3, 7, 'Abrigo de segunda mano', 'Abrigo de lana en excelente estado, talla L.', 18000, 'L', 'GOOD', 'Lana', NULL, 'ACTIVE', '2026-09-11T12:00:00Z', '2026-09-11T12:00:00Z', NULL),
    (9, 4, 3, 'Taller de bordado (4 clases)', 'Aprende puntos básicos y crea tu propio parche. Incluye materiales.', 15000, NULL, NULL, NULL, NULL, 'ACTIVE', '2026-09-22T12:00:00Z', '2026-09-22T12:00:00Z', NULL),
    (10, 6, 6, 'Polerón de algodón recuperado', 'Polerón unisex hecho con algodón sobrante de fábrica.', 22000, 'L', 'NEW', 'Algodón', NULL, 'ACTIVE', '2026-09-27T12:00:00Z', '2026-09-27T12:00:00Z', NULL),
    (11, 6, 5, 'Pack de 3 scrunchies', 'Coles de pelo hechas con retazos de seda y algodón.', 6000, 'ONE_SIZE', 'NEW', 'Seda y algodón', NULL, 'ACTIVE', '2026-09-28T12:00:00Z', '2026-09-28T12:00:00Z', NULL),
    (12, 3, 7, 'Blusa vintage', 'Blusa de los 80 con una pequeña mancha en el puño que casi no se nota.', 7000, 'S', 'WITH_DETAILS', 'Viscosa', NULL, 'ACTIVE', '2026-10-02T12:00:00Z', '2026-10-02T12:00:00Z', NULL),
    (13, 7, 7, 'Jeans tiro alto', 'Jeans de segunda mano, lavado claro.', 12000, 'M', 'GOOD', 'Mezclilla', NULL, 'ACTIVE', '2026-09-29T12:00:00Z', '2026-09-29T12:00:00Z', NULL),
    (14, 10, 4, 'Cambio de cierre', 'Cambiamos cierres de chaquetas y pantalones en el día.', 6000, NULL, NULL, NULL, 'REPAIR', 'ACTIVE', '2026-09-19T12:00:00Z', '2026-09-19T12:00:00Z', NULL),
    (15, 9, 5, 'Aros de tela', 'Aros livianos forrados con retazos de tela.', 5000, 'ONE_SIZE', 'NEW', 'Algodón', NULL, 'ACTIVE', '2026-09-13T12:00:00Z', '2026-09-13T12:00:00Z', NULL),
    -- Deleted by its owner: kept for history, never listed.
    (16, 1, 4, 'Teñido de prendas', 'Teñimos prendas desteñidas con tintes naturales.', 7000, NULL, NULL, NULL, 'REPAIR', 'ACTIVE', '2026-09-10T12:00:00Z', '2026-10-01T12:00:00Z', '2026-10-01T12:00:00Z')
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO product_photo (photo_id, product_id, url, sort_order, created_at) VALUES
    (1, 1, 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80', 1, '2026-09-04T12:00:00Z'),
    (2, 1, 'https://images.unsplash.com/photo-1582552938357-32b906df40cb?auto=format&fit=crop&w=800&q=80', 2, '2026-09-04T12:00:00Z'),
    (3, 2, 'https://images.unsplash.com/photo-1604014237800-1c9102c219da?auto=format&fit=crop&w=800&q=80', 1, '2026-09-06T12:00:00Z'),
    (4, 3, 'https://images.unsplash.com/photo-1593030761757-71fae45fa0e7?auto=format&fit=crop&w=800&q=80', 1, '2026-09-07T12:00:00Z'),
    (5, 4, 'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80', 1, '2026-09-06T12:00:00Z'),
    (6, 4, 'https://images.unsplash.com/photo-1597484661643-2f5fef640dd1?auto=format&fit=crop&w=800&q=80', 2, '2026-09-06T12:00:00Z'),
    (7, 4, 'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80', 3, '2026-09-06T12:00:00Z'),
    (8, 5, 'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=800&q=80', 1, '2026-09-09T12:00:00Z'),
    (9, 5, 'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80', 2, '2026-09-09T12:00:00Z'),
    (10, 5, 'https://images.unsplash.com/photo-1604176354204-9268737828e4?auto=format&fit=crop&w=800&q=80', 3, '2026-09-09T12:00:00Z'),
    (11, 6, 'https://images.unsplash.com/photo-1584100936595-c0654b55a2e2?auto=format&fit=crop&w=800&q=80', 1, '2026-09-10T12:00:00Z'),
    (12, 7, 'https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&w=800&q=80', 1, '2026-09-09T12:00:00Z'),
    (13, 7, 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=800&q=80', 2, '2026-09-09T12:00:00Z'),
    (14, 8, 'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=800&q=80', 1, '2026-09-11T12:00:00Z'),
    (15, 8, 'https://images.unsplash.com/photo-1490481651871-ab68de25d43d?auto=format&fit=crop&w=800&q=80', 2, '2026-09-11T12:00:00Z'),
    (16, 9, 'https://images.unsplash.com/photo-1618221195710-dd6b41faaea6?auto=format&fit=crop&w=800&q=80', 1, '2026-09-22T12:00:00Z'),
    (17, 10, 'https://images.unsplash.com/photo-1556905055-8f358a7a47b2?auto=format&fit=crop&w=800&q=80', 1, '2026-09-27T12:00:00Z'),
    (18, 11, 'https://images.unsplash.com/photo-1597484661643-2f5fef640dd1?auto=format&fit=crop&w=800&q=80', 1, '2026-09-28T12:00:00Z'),
    (19, 12, 'https://images.unsplash.com/photo-1515886657613-9f3515b0c78f?auto=format&fit=crop&w=800&q=80', 1, '2026-10-02T12:00:00Z'),
    (20, 13, 'https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80', 1, '2026-09-29T12:00:00Z'),
    (21, 14, 'https://images.unsplash.com/photo-1582552938357-32b906df40cb?auto=format&fit=crop&w=800&q=80', 1, '2026-09-19T12:00:00Z'),
    (22, 15, 'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80', 1, '2026-09-13T12:00:00Z')
ON CONFLICT (photo_id) DO NOTHING;

INSERT INTO conversation (conversation_id, initiator_id, business_id, created_at, blocked_at, blocked_by) VALUES
    (1, 8, 1, '2026-09-12T15:00:00Z', NULL, NULL),
    (2, 9, 2, '2026-09-14T10:00:00Z', NULL, NULL),
    (3, 8, 3, '2026-09-15T10:00:00Z', NULL, NULL),
    -- Blocked by the entrepreneur after spam: nobody can send until it is unblocked.
    (4, 10, 1, '2026-10-01T10:00:00Z', '2026-10-02T09:00:00Z', 3),
    -- Harassment that ended in a suspension.
    (5, 18, 2, '2026-09-28T20:00:00Z', NULL, NULL),
    (6, 11, 3, '2026-10-05T15:00:00Z', NULL, NULL),
    -- With a business whose owner deleted the account.
    (7, 9, 9, '2026-09-20T10:00:00Z', NULL, NULL),
    -- Unread messages waiting for the entrepreneur.
    (8, 11, 6, '2026-10-28T18:00:00Z', NULL, NULL),
    -- Started by a customer who later deleted the account.
    (9, 19, 2, '2026-09-30T12:00:00Z', NULL, NULL)
ON CONFLICT (conversation_id) DO NOTHING;

INSERT INTO message (message_id, conversation_id, sender_id, client_uuid, product_id, content, sent_at, read_at) VALUES
    (1, 1, 8, '624ceaa7-be04-5633-8dd8-fdc3d41a9c4b', NULL, 'Hola, ¿arreglan cierres de chaquetas de cuero?', '2026-09-12T15:00:00Z', '2026-09-12T16:00:00Z'),
    (2, 1, 3, '28ccb839-7539-5a85-bed6-54b80b24e0c7', NULL, '¡Hola Martín! Sí, los cambiamos. Depende del largo, ¿me mandas una foto?', '2026-09-12T16:00:00Z', '2026-09-12T17:00:00Z'),
    (3, 1, 8, 'cd9f912b-8299-5e7b-9773-801542e53ab6', NULL, 'Claro, te la envío mañana. ¿Cuánto demora?', '2026-09-12T18:00:00Z', '2026-09-12T19:00:00Z'),
    (4, 1, 3, '02ade0c3-68e3-5d85-bca1-a9a60947cda6', NULL, 'Entre 5 y 7 días hábiles. Te espero.', '2026-09-13T09:00:00Z', NULL),
    (5, 2, 9, 'df09fc60-f738-56c9-a751-270bd510a56b', 5, 'Hola! ¿La chaqueta patchwork sigue disponible?', '2026-09-14T10:00:00Z', '2026-09-14T11:00:00Z'),
    (6, 2, 4, 'c0ead8f5-86ef-5ca8-ada7-32bac3d567fc', NULL, 'Hola Sofi, sí, está disponible. Es talla M.', '2026-09-14T11:00:00Z', '2026-09-14T12:00:00Z'),
    (7, 2, 9, '60bf6484-1724-502b-a570-946f91a40650', NULL, 'Perfecto, ¿puedo pasar a verla este fin de semana?', '2026-09-14T13:00:00Z', NULL),
    (8, 3, 8, 'f84ce730-be3f-5674-85b0-af0c5871ed97', 7, 'Buenas, ¿tienen el vestido verde para el 20 de octubre?', '2026-09-15T10:00:00Z', '2026-09-15T11:00:00Z'),
    (9, 3, 5, 'c502b0a1-7c7a-5d82-8d96-b35594106aca', NULL, 'Hola Martín, está libre esa fecha. Te lo reservo.', '2026-09-15T12:00:00Z', '2026-09-15T13:00:00Z'),
    (10, 4, 10, md5('seed-message-10')::uuid, NULL, 'Hola, ¿hacen descuentos por volumen?', '2026-10-01T10:00:00Z', '2026-10-01T11:00:00Z'),
    (11, 4, 3, md5('seed-message-11')::uuid, NULL, 'Hola Pablo, depende de la prenda. ¿Qué necesitas?', '2026-10-01T11:00:00Z', '2026-10-01T11:30:00Z'),
    (12, 4, 10, md5('seed-message-12')::uuid, NULL, 'Compra seguidores para tu taller en seguidores-baratos.example', '2026-10-01T11:30:00Z', '2026-10-01T12:00:00Z'),
    (13, 4, 10, md5('seed-message-13')::uuid, NULL, 'Oferta solo por hoy en seguidores-baratos.example', '2026-10-02T08:00:00Z', '2026-10-02T09:00:00Z'),
    (14, 5, 18, md5('seed-message-14')::uuid, 5, 'Hola, ¿la chaqueta todavía está?', '2026-09-28T20:00:00Z', '2026-09-28T20:30:00Z'),
    (15, 5, 4, md5('seed-message-15')::uuid, NULL, 'Hola Bruno, sí, está disponible.', '2026-09-28T20:30:00Z', '2026-09-28T21:00:00Z'),
    (16, 5, 18, md5('seed-message-16')::uuid, NULL, 'Respóndeme rápido o te dejo una mala reseña en todos lados.', '2026-09-28T21:30:00Z', '2026-09-28T22:00:00Z'),
    (17, 5, 18, md5('seed-message-17')::uuid, NULL, 'Sé dónde queda tu taller.', '2026-09-29T08:00:00Z', '2026-09-29T09:00:00Z'),
    (18, 6, 11, md5('seed-message-18')::uuid, NULL, '¿Arriendan disfraces para niños?', '2026-10-05T15:00:00Z', '2026-10-05T16:00:00Z'),
    (19, 6, 5, md5('seed-message-19')::uuid, NULL, 'Por ahora solo ropa de fiesta para adultos, ¡pero pronto!', '2026-10-05T16:00:00Z', '2026-10-05T16:30:00Z'),
    (20, 6, 11, md5('seed-message-20')::uuid, NULL, 'Ok, gracias.', '2026-10-05T16:30:00Z', '2026-10-05T17:00:00Z'),
    (21, 7, 9, md5('seed-message-21')::uuid, 15, 'Hola, ¿tienen aros en tonos azules?', '2026-09-20T10:00:00Z', '2026-09-20T11:00:00Z'),
    (22, 7, 15, md5('seed-message-22')::uuid, NULL, 'Sí, me quedan dos pares. Te los guardo.', '2026-09-20T11:00:00Z', '2026-09-20T12:00:00Z'),
    (23, 8, 11, md5('seed-message-23')::uuid, 10, 'Hola Lucía, ¿el polerón viene en talla M?', '2026-10-28T18:00:00Z', NULL),
    (24, 8, 11, md5('seed-message-24')::uuid, 11, 'También me interesan los scrunchies.', '2026-10-28T18:05:00Z', NULL),
    (25, 9, 19, md5('seed-message-25')::uuid, NULL, '¿Hacen bolsos a pedido?', '2026-09-30T12:00:00Z', '2026-09-30T13:00:00Z'),
    (26, 9, 4, md5('seed-message-26')::uuid, NULL, 'Sí, cuéntame qué tamaño buscas.', '2026-09-30T13:00:00Z', NULL)
ON CONFLICT (message_id) DO NOTHING;

INSERT INTO conversation_report (report_id, conversation_id, reporter_id, message_id, reason, detail, status, created_at, resolved_by, resolved_at, resolution_note) VALUES
    (1, 4, 3, 12, 'SPAM', 'Manda enlaces para comprar seguidores.', 'OPEN', '2026-10-02T09:05:00Z', NULL, NULL, NULL),
    (2, 5, 4, 17, 'HARASSMENT', 'Dice que sabe dónde queda mi taller.', 'RESOLVED', '2026-09-29T09:10:00Z', 1, '2026-09-30T10:00:00Z', 'Cuenta suspendida por amenazas.'),
    (3, 6, 5, 18, 'OTHER', 'No sé si esta consulta es spam.', 'DISMISSED', '2026-10-06T10:00:00Z', 2, '2026-10-07T10:00:00Z', 'Consulta legítima, sin infracción.'),
    -- Reported by a customer, without pointing at a message.
    (4, 3, 8, NULL, 'SCAM', 'Me pidieron transferir antes de ver el vestido.', 'OPEN', '2026-10-12T15:00:00Z', NULL, NULL, NULL)
ON CONFLICT (report_id) DO NOTHING;

INSERT INTO outreach_content (content_id, author_id, type, title, body, image_url, event_at, event_location, created_at, updated_at, deleted_at) VALUES
    (1, 1, 'TEXT', NULL, 'Bienvenidas a Textil Circular, el directorio de emprendimientos que le dan una segunda vida a la ropa y a las telas.', NULL, NULL, NULL, '2026-09-01T12:00:00Z', '2026-09-01T12:00:00Z', NULL),
    (2, 1, 'ARTICLE', '5 formas de alargar la vida de tu ropa', '1. Lava en frío. 2. Repara antes de que se agrande el rasgado. 3. Intercambia con amigas. 4. Dona lo que no uses. 5. Compra de segunda mano antes que nuevo.', 'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=1000&q=80', NULL, NULL, '2026-09-08T12:00:00Z', '2026-09-08T12:00:00Z', NULL),
    (3, 1, 'IMAGE', NULL, NULL, 'https://images.unsplash.com/photo-1512436991641-6745cdb1723f?auto=format&fit=crop&w=1000&q=80', NULL, NULL, '2026-09-15T12:00:00Z', '2026-09-15T12:00:00Z', NULL),
    (4, 1, 'EVENT', 'Feria de segunda mano', 'Más de 20 emprendimientos de la región metropolitana.', 'https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=1000&q=80', now() + interval '12 days', 'Parque Bustamante, Providencia', '2026-09-18T12:00:00Z', '2026-09-18T12:00:00Z', NULL),
    (5, 1, 'TEXT', NULL, 'Este mes sumamos 3 emprendimientos nuevos al directorio. ¡Descúbrelos!', NULL, NULL, NULL, '2026-09-25T12:00:00Z', '2026-09-25T12:00:00Z', NULL),
    (6, 2, 'EVENT', 'Taller abierto de reparación', 'Trae una prenda rota y aprende a repararla.', NULL, now() - interval '20 days', 'Biblioteca Municipal de Ñuñoa', '2026-10-01T12:00:00Z', '2026-10-01T12:00:00Z', NULL),
    -- Deleted by an administrator: out of the feed.
    (7, 2, 'TEXT', NULL, 'Mensaje de prueba publicado por error.', NULL, NULL, NULL, '2026-10-03T12:00:00Z', '2026-10-03T12:00:00Z', '2026-10-03T13:00:00Z')
ON CONFLICT (content_id) DO NOTHING;

-- Usage events for the admin metrics panel. Occurred_at is relative to now() so the
-- "last 30/90 days" windows always show data in a fresh dev database. On re-runs the
-- conflict clause keeps the original rows untouched.
INSERT INTO analytics_event (event_id, type, actor_user_id, business_id, region_id, commune_id, service_id, search_term, occurred_at) VALUES
    (1, 'DIRECTORY_SEARCH', 8, NULL, 7, 102, 4, 'arreglo de jeans', now() - interval '1 day'),
    (2, 'DIRECTORY_SEARCH', 9, NULL, 7, 102, NULL, 'costura Ñuñoa', now() - interval '2 days'),
    (3, 'DIRECTORY_SEARCH', NULL, NULL, 6, 45, 2, 'bolsos de retazos', now() - interval '4 days'),
    (4, 'DIRECTORY_SEARCH', 10, NULL, 11, 219, 7, 'ropa de segunda mano', now() - interval '7 days'),
    (5, 'DIRECTORY_SEARCH', NULL, NULL, 7, NULL, 5, 'parches bordados', now() - interval '10 days'),
    (6, 'DIRECTORY_SEARCH', 11, NULL, 6, 45, 6, 'chaqueta patchwork', now() - interval '14 days'),
    (7, 'DIRECTORY_SEARCH', 8, NULL, 11, NULL, 1, 'arriendo vestido de fiesta', now() - interval '18 days'),
    (8, 'DIRECTORY_SEARCH', NULL, NULL, NULL, NULL, NULL, 'taller de bordado', now() - interval '23 days'),
    (9, 'DIRECTORY_SEARCH', 9, NULL, 7, 102, 4, 'arreglos de ropa', now() - interval '27 days'),
    (10, 'DIRECTORY_SEARCH', NULL, NULL, 7, 102, NULL, 'retazos de tela', now() - interval '33 days'),
    (11, 'DIRECTORY_SEARCH', 10, NULL, 6, 45, 2, 'diseño a pedido', now() - interval '40 days'),
    (12, 'DIRECTORY_SEARCH', NULL, NULL, 11, 219, 7, 'ropa usada', now() - interval '52 days'),
    (13, 'DIRECTORY_SEARCH', NULL, NULL, 7, NULL, NULL, 'taller telar', now() - interval '66 days'),
    (14, 'DIRECTORY_SEARCH', 11, NULL, 6, NULL, NULL, 'moda reciclada', now() - interval '80 days'),
    (15, 'BUSINESS_VIEW', 8, 1, NULL, NULL, NULL, NULL, now() - interval '1 day'),
    (16, 'BUSINESS_VIEW', 9, 1, NULL, NULL, NULL, NULL, now() - interval '3 days'),
    (17, 'BUSINESS_VIEW', NULL, 1, NULL, NULL, NULL, NULL, now() - interval '5 days'),
    (18, 'BUSINESS_VIEW', 10, 1, NULL, NULL, NULL, NULL, now() - interval '9 days'),
    (19, 'BUSINESS_VIEW', 11, 1, NULL, NULL, NULL, NULL, now() - interval '13 days'),
    (20, 'BUSINESS_VIEW', NULL, 1, NULL, NULL, NULL, NULL, now() - interval '19 days'),
    (21, 'BUSINESS_VIEW', 8, 1, NULL, NULL, NULL, NULL, now() - interval '25 days'),
    (22, 'BUSINESS_VIEW', NULL, 1, NULL, NULL, NULL, NULL, now() - interval '31 days'),
    (23, 'BUSINESS_VIEW', 9, 2, NULL, NULL, NULL, NULL, now() - interval '2 days'),
    (24, 'BUSINESS_VIEW', NULL, 2, NULL, NULL, NULL, NULL, now() - interval '6 days'),
    (25, 'BUSINESS_VIEW', 10, 2, NULL, NULL, NULL, NULL, now() - interval '11 days'),
    (26, 'BUSINESS_VIEW', 11, 2, NULL, NULL, NULL, NULL, now() - interval '16 days'),
    (27, 'BUSINESS_VIEW', NULL, 2, NULL, NULL, NULL, NULL, now() - interval '22 days'),
    (28, 'BUSINESS_VIEW', 8, 2, NULL, NULL, NULL, NULL, now() - interval '44 days'),
    (29, 'BUSINESS_VIEW', NULL, 3, NULL, NULL, NULL, NULL, now() - interval '4 days'),
    (30, 'BUSINESS_VIEW', 8, 3, NULL, NULL, NULL, NULL, now() - interval '8 days'),
    (31, 'BUSINESS_VIEW', NULL, 3, NULL, NULL, NULL, NULL, now() - interval '15 days'),
    (32, 'BUSINESS_VIEW', 9, 3, NULL, NULL, NULL, NULL, now() - interval '28 days')
ON CONFLICT (event_id) DO NOTHING;

INSERT INTO analytics_event (event_id, type, actor_user_id, business_id, product_id, occurred_at) VALUES
    (33, 'PRODUCT_VIEW', 8, 1, 1, now() - interval '1 day'),
    (34, 'PRODUCT_VIEW', 9, 1, 1, now() - interval '3 days'),
    (35, 'PRODUCT_VIEW', NULL, 1, 2, now() - interval '6 days'),
    (36, 'PRODUCT_VIEW', 10, 2, 5, now() - interval '2 days'),
    (37, 'PRODUCT_VIEW', 11, 2, 5, now() - interval '5 days'),
    (38, 'PRODUCT_VIEW', NULL, 2, 4, now() - interval '12 days'),
    (39, 'PRODUCT_VIEW', 8, 3, 7, now() - interval '4 days'),
    (40, 'PRODUCT_VIEW', NULL, 3, 8, now() - interval '20 days'),
    (41, 'PRODUCT_VIEW', 11, 6, 10, now() - interval '7 days'),
    (42, 'PRODUCT_VIEW', NULL, 1, 1, now() - interval '45 days'),
    (43, 'CONTACT_CLICK', 8, 1, NULL, now() - interval '1 day'),
    (44, 'CONTACT_CLICK', 9, 2, NULL, now() - interval '2 days'),
    (45, 'CONTACT_CLICK', 11, 2, NULL, now() - interval '9 days'),
    (46, 'CONTACT_CLICK', 8, 3, NULL, now() - interval '4 days'),
    (47, 'CONTACT_CLICK', 11, 6, NULL, now() - interval '6 days'),
    (48, 'CONTACT_CLICK', 10, 1, NULL, now() - interval '17 days'),
    (49, 'CONTACT_CLICK', 9, 1, NULL, now() - interval '50 days')
ON CONFLICT (event_id) DO NOTHING;

-- Directory volume: 24 more approved businesses (ids 100-123), one product each, so the directory
-- and the admin user list need a second page at the default size of 20. RUTs carry a valid check digit.
INSERT INTO user_account (user_id, email, password_hash, role, alias, status, consent_at, created_at, updated_at, deactivated_at, deleted_at)
SELECT i, format('emprendimiento%s@example.cl', i), '{bcrypt}$2a$10$Hg7tsJ1wosJ9xuULwl.wJuW2Z502kQnTgO2h.rPBGUJIDKPTXIUJm', 'ENTREPRENEUR', NULL, 'ACTIVE',
       timestamptz '2026-08-20T12:00:00Z' + (i - 100) * interval '1 day',
       timestamptz '2026-08-20T12:00:00Z' + (i - 100) * interval '1 day',
       timestamptz '2026-08-20T12:00:00Z' + (i - 100) * interval '1 day', NULL, NULL
FROM generate_series(100, 123) AS i
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO entrepreneur (user_id, age_range_id, gender_id, rut, first_name, last_name, anonymized_at)
SELECT i, 2 + i % 5, 1 + i % 4,
       r.n || '-' || CASE d.dv WHEN 11 THEN '0' WHEN 10 THEN 'K' ELSE d.dv::text END,
       (ARRAY['Paula', 'Constanza', 'Francisca', 'Catalina', 'Josefa', 'Antonia', 'Macarena', 'Carolina',
              'Isidora', 'Trinidad', 'Gabriela', 'Bárbara', 'Florencia', 'Renata', 'Amanda', 'Emilia',
              'Martina', 'Agustina', 'Pilar', 'Claudia', 'Natalia', 'Verónica', 'Jorge', 'Matías'])[i - 99],
       (ARRAY['González', 'Muñoz', 'Rojas', 'Díaz', 'Pérez', 'Soto', 'Contreras', 'Silva', 'Martínez',
              'Sepúlveda', 'Morales', 'Fuentes'])[1 + i % 12],
       NULL
FROM generate_series(100, 123) AS i,
     LATERAL (SELECT 20100000 + i * 1237 AS n) AS r,
     -- Módulo 11 check digit.
     LATERAL (SELECT 11 - sum(substr(reverse(r.n::text), k, 1)::int * (2 + (k - 1) % 6)) % 11 AS dv
              FROM generate_series(1, length(r.n::text)) AS k) AS d
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO business (business_id, user_id, commune_id, name, description, photo_url, status, review_round, approved_at, hidden_at, created_at, updated_at, deleted_at)
SELECT i, i,
       (ARRAY[83, 105, 101, 51, 30, 12, 168, 284, 296, 219, 252, 102])[1 + i % 12],
       (ARRAY['Aguja y Dedal', 'Remendado', 'Trapo Nuevo', 'La Costurera del Cerro', 'Hilván', 'Tela Viva',
              'Ropa con Historia', 'Botón Perdido', 'Retal', 'Zurcido Fino', 'El Baúl de la Abuela', 'Puntada Verde',
              'Lana Circular', 'Overol Taller', 'Mezclilla Viva', 'Costuras del Sur', 'Patchwork Andino',
              'Vuelve a Vestir', 'Taller Hilo Rojo', 'Estampa Reciclada', 'Clóset Viajero', 'Bordado Austral',
              'Telar Urbano', 'Prenda Eterna'])[i - 99],
       -- Indexed by the business's only service (1 + i % 7), so the text matches it.
       (ARRAY['Arriendo de ropa de fiesta y disfraces para no comprar algo que usarás una sola vez.',
              'Prendas a pedido hechas con telas en desuso. Cuéntanos tu idea y la diseñamos contigo.',
              'Talleres de costura, bordado y reparación para todas las edades.',
              'Reparamos y ajustamos ropa y accesorios para que duren más.',
              'Accesorios hechos con retazos de tela: bolsos, coles, aros y más.',
              'Ropa nueva confeccionada con telas recuperadas.',
              'Ropa de segunda mano seleccionada, lavada y lista para usar.'])[1 + i % 7],
       (ARRAY['https://images.unsplash.com/photo-1528459801416-a9e53bbf4e17?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1584917865442-de89df76afd3?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1489987707025-afc232f7ea0f?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1606760227091-3dd870d97f1d?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1558769132-cb1aea458c5e?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1576995853123-5a10305d93c0?auto=format&fit=crop&w=800&q=80'])[1 + i % 6],
       'APPROVED', 1,
       timestamptz '2026-08-22T12:00:00Z' + (i - 100) * interval '1 day', NULL,
       timestamptz '2026-08-20T12:00:00Z' + (i - 100) * interval '1 day',
       timestamptz '2026-08-22T12:00:00Z' + (i - 100) * interval '1 day', NULL
FROM generate_series(100, 123) AS i
ON CONFLICT (business_id) DO NOTHING;

INSERT INTO business_service (business_id, service_id, sort_order)
SELECT i, 1 + i % 7, 1 FROM generate_series(100, 123) AS i
WHERE NOT EXISTS (SELECT 1 FROM business_service WHERE business_id = i);

-- Both administrators approved each one in round 1.
INSERT INTO business_review (review_id, business_id, reviewer_id, review_round, decision, rejection_reason, comment, reviewed_at)
SELECT 200 + (i - 100) * 2 + a - 1, i, a, 1, 'APPROVED', NULL, NULL,
       timestamptz '2026-08-21T12:00:00Z' + (i - 100) * interval '1 day' + a * interval '1 hour'
FROM generate_series(100, 123) AS i, generate_series(1, 2) AS a
ON CONFLICT (review_id) DO NOTHING;

-- Product arrays are indexed by service id (1-7).
INSERT INTO product (product_id, business_id, service_id, title, description, price, size, condition, material, offering_type, status, created_at, updated_at, deleted_at)
SELECT i, i, v.s,
       (ARRAY['Arriendo de disfraz', 'Chaqueta a pedido', 'Taller de costura básica', 'Basta de pantalón',
              'Bolso de retazos', 'Falda de tela recuperada', 'Chaleco de segunda mano'])[v.s],
       'Consulta disponibilidad y plazos por mensaje.',
       5000 + (i % 8) * 2500,
       (ARRAY['M', NULL, NULL, NULL, 'ONE_SIZE', 'S', 'XL'])[v.s],
       (ARRAY['GOOD', NULL, NULL, NULL, 'NEW', 'NEW', 'WITH_DETAILS'])[v.s],
       (ARRAY['Poliéster', 'Mezclilla', NULL, NULL, 'Algodón', 'Lino', 'Lana'])[v.s],
       (ARRAY[NULL, 'MADE_TO_ORDER', NULL, 'REPAIR', NULL, NULL, NULL])[v.s],
       'ACTIVE', v.t, v.t, NULL
FROM generate_series(100, 123) AS i,
     LATERAL (SELECT 1 + i % 7 AS s, timestamptz '2026-08-23T12:00:00Z' + (i - 100) * interval '1 day' AS t) AS v
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO product_photo (photo_id, product_id, url, sort_order, created_at)
SELECT i, i,
       (ARRAY['https://images.unsplash.com/photo-1541099649105-f69ad21f3246?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1544816155-12df9643f363?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1566174053879-31528523f8ae?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1544441893-675973e31985?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1551028719-00167b16eac5?auto=format&fit=crop&w=800&q=80',
              'https://images.unsplash.com/photo-1604176354204-9268737828e4?auto=format&fit=crop&w=800&q=80'])[1 + i % 6],
       1, timestamptz '2026-08-23T12:00:00Z' + (i - 100) * interval '1 day'
FROM generate_series(100, 123) AS i
ON CONFLICT (photo_id) DO NOTHING;
