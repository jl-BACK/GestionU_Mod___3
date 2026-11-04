CREATE TABLE reservation_order (
  reservation_id VARCHAR(100) PRIMARY KEY,
  last_event_at TIMESTAMP(6) NOT NULL,
  status VARCHAR(20) NOT NULL
);