
CREATE TABLE sensor_values (
    id BIGSERIAL PRIMARY KEY,
    sensor_type VARCHAR(32) NOT NULL,
    sensor_value DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP NOT NULL,
    value_unit VARCHAR(8)
);

CREATE TABLE dispenser_schedule (
    id BIGSERIAL PRIMARY KEY,
    distribution_time TIME,
    label VARCHAR(64)
)
