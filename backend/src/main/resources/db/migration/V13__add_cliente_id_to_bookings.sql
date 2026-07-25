ALTER TABLE bookings ADD COLUMN cliente_id BIGINT REFERENCES clientes(id);

CREATE INDEX idx_bookings_cliente_id ON bookings(cliente_id);
