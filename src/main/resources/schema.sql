-- CanKhata schema (MySQL 8). Safe to run many times: every statement is IF NOT EXISTS / INSERT IGNORE.
-- Money is stored in paise (BIGINT). Dates in DATETIME. Tamil text works because of utf8mb4.

CREATE TABLE IF NOT EXISTS roles (
  id   TINYINT PRIMARY KEY,
  name VARCHAR(20) NOT NULL,
  UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO roles (id, name) VALUES (1, 'OWNER'), (2, 'DELIVERY_BOY'), (3, 'CUSTOMER');

CREATE TABLE IF NOT EXISTS users (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  role_id       TINYINT      NOT NULL,
  name          VARCHAR(100) NOT NULL,
  phone         VARCHAR(15)  NOT NULL,
  email         VARCHAR(150) NULL,
  password_hash VARCHAR(100) NOT NULL,
  active        BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_users_phone (phone),
  UNIQUE KEY uq_users_email (email),
  KEY idx_users_role (role_id),
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS routes (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  name             VARCHAR(100) NOT NULL,
  delivery_boy_id  BIGINT NULL,
  UNIQUE KEY uq_routes_name (name),
  CONSTRAINT fk_routes_boy FOREIGN KEY (delivery_boy_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customers (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id             BIGINT NULL,
  name                VARCHAR(100) NOT NULL,
  phone               VARCHAR(15)  NOT NULL,
  address             VARCHAR(255) NOT NULL,
  landmark            VARCHAR(150) NULL,
  route_id            BIGINT NULL,
  rate_override_paise BIGINT NULL,
  credit_limit_paise  BIGINT NOT NULL DEFAULT 0,
  active              BOOLEAN NOT NULL DEFAULT TRUE,
  created_at          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_customers_phone (phone),
  UNIQUE KEY uq_customers_user (user_id),
  KEY idx_customers_route (route_id),
  CONSTRAINT fk_customers_user  FOREIGN KEY (user_id)  REFERENCES users (id),
  CONSTRAINT fk_customers_route FOREIGN KEY (route_id) REFERENCES routes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS can_types (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  name          VARCHAR(50) NOT NULL,
  price_paise   BIGINT NOT NULL,
  deposit_paise BIGINT NOT NULL,
  UNIQUE KEY uq_can_types_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Sample can type so the app is usable on day one. The owner edits the prices later.
INSERT IGNORE INTO can_types (id, name, price_paise, deposit_paise) VALUES (1, '20 Litre Can', 4000, 25000);

CREATE TABLE IF NOT EXISTS subscriptions (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id   BIGINT NOT NULL,
  can_type_id   BIGINT NOT NULL,
  qty           INT NOT NULL,
  frequency     VARCHAR(10) NOT NULL,           -- DAILY, ALTERNATE, WEEKDAYS
  weekday_mask  TINYINT NOT NULL DEFAULT 127,   -- bit 0 = Monday ... bit 6 = Sunday
  start_date    DATE NOT NULL,
  end_date      DATE NULL,
  active        BOOLEAN NOT NULL DEFAULT TRUE,
  KEY idx_sub_customer (customer_id, active),
  CONSTRAINT fk_sub_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_sub_type     FOREIGN KEY (can_type_id) REFERENCES can_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skip_days (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  subscription_id BIGINT NOT NULL,
  skip_date       DATE NOT NULL,
  created_by      BIGINT NOT NULL,
  UNIQUE KEY uq_skip (subscription_id, skip_date),
  CONSTRAINT fk_skip_sub  FOREIGN KEY (subscription_id) REFERENCES subscriptions (id),
  CONSTRAINT fk_skip_user FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS daily_sheets (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  sheet_date DATE NOT NULL,
  status     VARCHAR(10) NOT NULL DEFAULT 'OPEN',   -- OPEN, CLOSED
  closed_by  BIGINT NULL,
  closed_at  DATETIME NULL,
  UNIQUE KEY uq_sheet_date (sheet_date),
  CONSTRAINT fk_sheet_user FOREIGN KEY (closed_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS deliveries (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  sheet_id             BIGINT NOT NULL,
  customer_id          BIGINT NOT NULL,
  can_type_id          BIGINT NOT NULL,
  delivery_boy_id      BIGINT NULL,
  planned_qty          INT NOT NULL,
  delivered_qty        INT NOT NULL DEFAULT 0,
  empties_collected    INT NOT NULL DEFAULT 0,
  unit_price_paise     BIGINT NOT NULL,
  cash_collected_paise BIGINT NOT NULL DEFAULT 0,
  status               VARCHAR(10) NOT NULL DEFAULT 'PLANNED',  -- PLANNED, DELIVERED, MISSED
  note                 VARCHAR(255) NULL,
  UNIQUE KEY uq_delivery (sheet_id, customer_id, can_type_id),
  KEY idx_delivery_boy (delivery_boy_id, sheet_id),
  KEY idx_delivery_customer (customer_id, status),
  CONSTRAINT fk_del_sheet    FOREIGN KEY (sheet_id)        REFERENCES daily_sheets (id),
  CONSTRAINT fk_del_customer FOREIGN KEY (customer_id)     REFERENCES customers (id),
  CONSTRAINT fk_del_type     FOREIGN KEY (can_type_id)     REFERENCES can_types (id),
  CONSTRAINT fk_del_boy      FOREIGN KEY (delivery_boy_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS van_loads (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  sheet_id        BIGINT NOT NULL,
  delivery_boy_id BIGINT NOT NULL,
  full_out        INT NOT NULL DEFAULT 0,
  full_back       INT NOT NULL DEFAULT 0,
  empties_in      INT NOT NULL DEFAULT 0,
  UNIQUE KEY uq_van (sheet_id, delivery_boy_id),
  CONSTRAINT fk_van_sheet FOREIGN KEY (sheet_id)        REFERENCES daily_sheets (id),
  CONSTRAINT fk_van_boy   FOREIGN KEY (delivery_boy_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Append-only: rows are only ever inserted. A mistake is fixed by inserting a reversing row.
CREATE TABLE IF NOT EXISTS can_ledger (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id BIGINT NOT NULL,
  can_type_id BIGINT NOT NULL,
  entry_type  VARCHAR(12) NOT NULL,   -- DELIVERED, RETURNED, LOST, DAMAGED, ADJUSTMENT
  qty         INT NOT NULL,           -- signed: +cans held by customer, -cans given back
  delivery_id BIGINT NULL,
  note        VARCHAR(255) NULL,
  created_by  BIGINT NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_ledger_customer (customer_id, can_type_id),
  KEY idx_ledger_created (created_at),
  CONSTRAINT fk_ledger_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_ledger_type     FOREIGN KEY (can_type_id) REFERENCES can_types (id),
  CONSTRAINT fk_ledger_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries (id),
  CONSTRAINT fk_ledger_user     FOREIGN KEY (created_by)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS deposit_txns (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id  BIGINT NOT NULL,
  txn_type     VARCHAR(10) NOT NULL,   -- COLLECTED, REFUNDED
  amount_paise BIGINT NOT NULL,
  cans         INT NOT NULL,
  created_by   BIGINT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_deposit_customer (customer_id),
  CONSTRAINT fk_dep_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_dep_user     FOREIGN KEY (created_by)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS invoices (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  customer_id   BIGINT NOT NULL,
  invoice_month CHAR(7) NOT NULL,      -- YYYY-MM
  total_paise   BIGINT NOT NULL,
  status        VARCHAR(10) NOT NULL DEFAULT 'OPEN',   -- OPEN, PAID
  generated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_invoice_month (customer_id, invoice_month),
  CONSTRAINT fk_inv_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  invoice_id    BIGINT NULL,
  customer_id   BIGINT NOT NULL,
  amount_paise  BIGINT NOT NULL,
  method        VARCHAR(5) NOT NULL,        -- CASH, UPI
  receipt_no    VARCHAR(40) NOT NULL,
  request_token VARCHAR(64) NOT NULL,       -- stops a double-click from saving the payment twice
  received_by   BIGINT NOT NULL,
  paid_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_payment_receipt (receipt_no),
  UNIQUE KEY uq_payment_token (request_token),
  KEY idx_payment_customer (customer_id),
  CONSTRAINT fk_pay_invoice  FOREIGN KEY (invoice_id)  REFERENCES invoices (id),
  CONSTRAINT fk_pay_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
  CONSTRAINT fk_pay_user     FOREIGN KEY (received_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NULL,
  action     VARCHAR(40)  NOT NULL,
  entity     VARCHAR(40)  NOT NULL,
  entity_id  BIGINT NULL,
  detail     VARCHAR(500) NULL,
  ip         VARCHAR(45)  NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_audit_entity (entity, entity_id),
  KEY idx_audit_created (created_at),
  CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notifications (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id    BIGINT NOT NULL,
  message    VARCHAR(300) NOT NULL,
  type       VARCHAR(20)  NOT NULL,
  read_flag  BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_notif_user (user_id, read_flag),
  CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
