
CREATE TABLE sensor_values (
    id BIGSERIAL PRIMARY KEY,
    sensor_type VARCHAR(32) NOT NULL,
    sensor_value DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP NOT NULL
);
