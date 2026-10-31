CREATE TABLE student_ref (
  student_code VARCHAR(40) PRIMARY KEY,
  name VARCHAR(180) NOT NULL
);

CREATE TABLE trust_score (
  student_code VARCHAR(40) PRIMARY KEY,
  score INT NOT NULL,
  last_infraction_at TIMESTAMP(6) NULL,
  last_recovery_at TIMESTAMP(6) NULL,
  version BIGINT NOT NULL DEFAULT 0,
  CONSTRAINT ck_trust_score_range CHECK (score >= 0 AND score <= 100)
);

CREATE TABLE score_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_code VARCHAR(40) NOT NULL,
  type VARCHAR(40) NOT NULL,
  effective_points INT NOT NULL,
  score_before INT NOT NULL,
  score_after INT NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  origin_type VARCHAR(80) NULL,
  origin_id VARCHAR(120) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  UNIQUE KEY uq_score_origin (student_code, type, origin_type, origin_id),
  KEY ix_score_history_student_date (student_code, created_at)
);

CREATE TABLE reservation_copy (
  reservation_id VARCHAR(100) PRIMARY KEY,
  origin VARCHAR(40) NOT NULL DEFAULT 'ESTUDIANTE',
  sanctionable BOOLEAN NOT NULL DEFAULT TRUE,
  holder_code VARCHAR(40) NOT NULL,
  holder_name VARCHAR(180) NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  resource_category VARCHAR(20) NOT NULL,
  resource_type VARCHAR(120) NULL,
  resource_location VARCHAR(180) NULL,
  faculty VARCHAR(180) NULL,
  starts_at TIMESTAMP(6) NOT NULL,
  ends_at TIMESTAMP(6) NOT NULL,
  loan_withdrawn_at TIMESTAMP(6) NULL,
  loan_due_at TIMESTAMP(6) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  score_at_due INT NULL,
  last_event_at TIMESTAMP(6) NULL,
  KEY ix_reservation_holder (holder_code, status, starts_at),
  KEY ix_reservation_overdue (resource_category, status, loan_due_at)
);

CREATE TABLE usage_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id VARCHAR(100) NOT NULL UNIQUE,
  student_code VARCHAR(40) NOT NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  resource_category VARCHAR(20) NOT NULL,
  resource_type VARCHAR(120) NULL,
  resource_location VARCHAR(180) NULL,
  faculty VARCHAR(180) NULL,
  due_at TIMESTAMP(6) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  started_at TIMESTAMP(6) NOT NULL,
  KEY ix_usage_student_status (student_code, status)
);

CREATE TABLE check_out (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  usage_id BIGINT NOT NULL UNIQUE,
  reservation_id VARCHAR(100) NOT NULL UNIQUE,
  student_code VARCHAR(40) NOT NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  resource_category VARCHAR(20) NOT NULL,
  resource_type VARCHAR(120) NULL,
  resource_location VARCHAR(180) NULL,
  faculty VARCHAR(180) NULL,
  checked_out_at TIMESTAMP(6) NOT NULL,
  result VARCHAR(20) NOT NULL,
  delay_minutes INT NOT NULL DEFAULT 0,
  has_damage BOOLEAN NOT NULL DEFAULT FALSE,
  damage_description VARCHAR(2000) NULL,
  ack_status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  ack_reason VARCHAR(500) NULL,
  accepted_from TIMESTAMP(6) NULL,
  due_at TIMESTAMP(6) NOT NULL,
  KEY ix_checkout_student_date (student_code, checked_out_at),
  KEY ix_checkout_result_date (result, checked_out_at)
);

CREATE TABLE overdue_deduction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id VARCHAR(100) NOT NULL,
  student_code VARCHAR(40) NOT NULL,
  deduction_date DATE NOT NULL,
  points INT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  UNIQUE KEY uq_overdue_student_day (student_code, deduction_date),
  KEY ix_overdue_reservation (reservation_id, deduction_date)
);

CREATE TABLE lost_loan (
  reservation_id VARCHAR(100) PRIMARY KEY,
  student_code VARCHAR(40) NOT NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  resource_type VARCHAR(120) NULL,
  declared_at TIMESTAMP(6) NOT NULL,
  days_late INT NOT NULL DEFAULT 7,
  charge_result VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE'
);

CREATE TABLE absence_confirmation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reservation_id VARCHAR(100) NOT NULL UNIQUE,
  student_code VARCHAR(40) NOT NULL,
  space_id VARCHAR(100) NOT NULL,
  space_name VARCHAR(180) NULL,
  starts_at TIMESTAMP(6) NOT NULL,
  verified_at TIMESTAMP(6) NOT NULL,
  confirmed_by VARCHAR(80) NOT NULL,
  note VARCHAR(1000) NULL,
  status VARCHAR(30) NOT NULL,
  reject_reason VARCHAR(500) NULL,
  accepted_from TIMESTAMP(6) NULL,
  points_deducted INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  KEY ix_absence_student_status (student_code, status),
  KEY ix_absence_space_date (space_id, starts_at)
);

CREATE TABLE sanction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_code VARCHAR(40) NOT NULL,
  type VARCHAR(40) NOT NULL DEFAULT 'INASISTENCIA',
  status VARCHAR(20) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  starts_on DATE NOT NULL,
  ends_on DATE NOT NULL,
  absence_confirmation_id BIGINT NULL UNIQUE,
  lifted_reason VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  KEY ix_sanction_student_status (student_code, status, starts_on, ends_on)
);

CREATE TABLE absence_annulment_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  confirmation_id BIGINT NOT NULL,
  annulled_by VARCHAR(80) NOT NULL,
  annulled_at TIMESTAMP(6) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  points_restored INT NOT NULL DEFAULT 0
);

CREATE TABLE damage_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  usage_id BIGINT NULL UNIQUE,
  reservation_id VARCHAR(100) NULL,
  student_code VARCHAR(40) NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  resource_category VARCHAR(20) NULL,
  resource_type VARCHAR(120) NULL,
  resource_location VARCHAR(180) NULL,
  faculty VARCHAR(180) NULL,
  reported_by VARCHAR(80) NOT NULL,
  description VARCHAR(2000) NOT NULL,
  evidence_path VARCHAR(500) NULL,
  reported_at TIMESTAMP(6) NOT NULL,
  issue_status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  charge_result VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
  KEY ix_damage_resource_status (resource_id, issue_status),
  KEY ix_damage_date (reported_at),
  KEY ix_damage_student_date (student_code, reported_at)
);

CREATE TABLE cost_catalog (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type VARCHAR(120) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  KEY ix_cost_type_active (resource_type, active)
);

CREATE TABLE charge (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  damage_report_id BIGINT NULL UNIQUE,
  lost_loan_reservation_id VARCHAR(100) NULL UNIQUE,
  student_code VARCHAR(40) NOT NULL,
  resource_id VARCHAR(100) NOT NULL,
  resource_name VARCHAR(180) NULL,
  reason VARCHAR(200) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  created_at TIMESTAMP(6) NOT NULL,
  KEY ix_charge_student_date (student_code, created_at),
  KEY ix_charge_status_date (status, created_at)
);

CREATE TABLE charge_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  charge_id BIGINT NOT NULL,
  changed_by VARCHAR(80) NOT NULL,
  previous_status VARCHAR(20) NOT NULL,
  new_status VARCHAR(20) NOT NULL,
  reason VARCHAR(1000) NULL,
  changed_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE unblock_audit (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_code VARCHAR(40) NOT NULL,
  unblocked_by VARCHAR(80) NOT NULL,
  unblocked_at TIMESTAMP(6) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  score_before INT NOT NULL,
  score_after INT NOT NULL,
  sanctions_lifted INT NOT NULL DEFAULT 0,
  KEY ix_unblock_student_date (student_code, unblocked_at)
);

CREATE TABLE system_configuration (
  config_key VARCHAR(100) PRIMARY KEY,
  value VARCHAR(500) NOT NULL,
  description VARCHAR(500) NOT NULL
);

CREATE TABLE outbox_message (
  id CHAR(36) PRIMARY KEY,
  topic VARCHAR(200) NOT NULL,
  event_type VARCHAR(120) NOT NULL,
  message_key VARCHAR(200) NOT NULL,
  payload MEDIUMTEXT NOT NULL,
  headers_json TEXT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  attempts INT NOT NULL DEFAULT 0,
  next_retry_at TIMESTAMP(6) NOT NULL,
  last_error VARCHAR(1000) NULL,
  created_at TIMESTAMP(6) NOT NULL,
  published_at TIMESTAMP(6) NULL,
  KEY ix_outbox_pending (status, next_retry_at, created_at)
);

CREATE TABLE processed_event (
  event_id VARCHAR(120) PRIMARY KEY,
  topic VARCHAR(200) NOT NULL,
  processed_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE inconsistency (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  aggregate_type VARCHAR(80) NOT NULL,
  aggregate_id VARCHAR(120) NOT NULL,
  description VARCHAR(1000) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  attempts INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  resolved_at TIMESTAMP(6) NULL,
  KEY ix_inconsistency_status (status, created_at)
);