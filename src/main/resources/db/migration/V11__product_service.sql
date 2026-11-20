-- Align the product entity with the serviceId property in docs/api/openapi.yaml.
ALTER TABLE product ADD COLUMN service_id bigint REFERENCES service (service_id);
CREATE INDEX ix_product_service ON product (service_id) WHERE service_id IS NOT NULL;
