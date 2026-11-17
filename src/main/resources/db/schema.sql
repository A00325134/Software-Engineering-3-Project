-- SQLite schema for the newsagent system. Keep deletion restrictive so billing
-- and delivery history cannot be removed accidentally.
CREATE TABLE IF NOT EXISTS customer (
    customer_id TEXT PRIMARY KEY CHECK (length(customer_id) BETWEEN 5 AND 15),
    name TEXT NOT NULL CHECK (length(trim(name)) BETWEEN 3 AND 50),
    phone_number TEXT NOT NULL CHECK (length(trim(phone_number)) BETWEEN 5 AND 20),
    address TEXT NOT NULL CHECK (length(trim(address)) BETWEEN 3 AND 100),
    eircode TEXT NOT NULL CHECK (length(eircode) = 7),
    delivery_area INTEGER NOT NULL CHECK (delivery_area BETWEEN 1 AND 24)
);

CREATE TABLE IF NOT EXISTS delivery_driver (
    driver_id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(driver_id)) > 0),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    password_hash TEXT NOT NULL CHECK (length(trim(password_hash)) > 0)
);

CREATE TABLE IF NOT EXISTS publication (
    publication_id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(publication_id)) > 0),
    name TEXT NOT NULL CHECK (length(trim(name)) BETWEEN 3 AND 50),
    stock_quantity INTEGER NOT NULL CHECK (stock_quantity BETWEEN 0 AND 1000),
    low_stock_threshold INTEGER NOT NULL DEFAULT 5 CHECK (low_stock_threshold = 5)
);

CREATE TABLE IF NOT EXISTS subscription (
    subscription_id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(subscription_id)) > 0),
    customer_id TEXT NOT NULL,
    publication_id TEXT NOT NULL,
    delivery_frequency TEXT NOT NULL CHECK (delivery_frequency IN ('MONTHLY')),
    paused INTEGER NOT NULL DEFAULT 0 CHECK (paused IN (0, 1)),
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (publication_id) REFERENCES publication(publication_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS bill (
    bill_reference TEXT PRIMARY KEY,
    customer_id TEXT NOT NULL,
    billing_month TEXT NOT NULL,
    monthly_total NUMERIC NOT NULL CHECK (monthly_total >= 0 AND length(CAST(monthly_total AS TEXT)) BETWEEN 1 AND 8),
    payment_status TEXT NOT NULL CHECK (payment_status IN ('PAID', 'UNPAID')),
    docket_reference TEXT,
    UNIQUE (customer_id, billing_month),
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (docket_reference) REFERENCES delivery_docket(docket_reference) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS bill_item (
    bill_reference TEXT NOT NULL,
    item_number INTEGER NOT NULL,
    item_name TEXT NOT NULL CHECK (length(trim(item_name)) BETWEEN 3 AND 50),
    delivery_date TEXT NOT NULL,
    amount NUMERIC NOT NULL CHECK (amount >= 0),
    delivery_reference TEXT,
    PRIMARY KEY (bill_reference, item_number),
    FOREIGN KEY (bill_reference) REFERENCES bill(bill_reference) ON DELETE RESTRICT,
    FOREIGN KEY (delivery_reference) REFERENCES delivery(delivery_reference) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS delivery_docket (
    docket_reference TEXT PRIMARY KEY,
    delivery_date TEXT NOT NULL,
    driver_id TEXT,
    delivery_area INTEGER NOT NULL CHECK (delivery_area BETWEEN 1 AND 24),
    FOREIGN KEY (driver_id) REFERENCES delivery_driver(driver_id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS delivery (
    delivery_reference TEXT PRIMARY KEY,
    docket_reference TEXT NOT NULL,
    subscription_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('DELIVERED', 'FAILED', 'OUT_OF_STOCK')),
    FOREIGN KEY (docket_reference) REFERENCES delivery_docket(docket_reference) ON DELETE RESTRICT,
    FOREIGN KEY (subscription_id) REFERENCES subscription(subscription_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_delivery_docket_date ON delivery_docket(delivery_date);
CREATE INDEX IF NOT EXISTS idx_delivery_subscription ON delivery(subscription_id);

CREATE TABLE IF NOT EXISTS out_of_stock_record (
    delivery_reference TEXT PRIMARY KEY,
    publication_id TEXT NOT NULL,
    recorded_at TEXT NOT NULL,
    note TEXT,
    FOREIGN KEY (delivery_reference) REFERENCES delivery(delivery_reference) ON DELETE RESTRICT,
    FOREIGN KEY (publication_id) REFERENCES publication(publication_id) ON DELETE RESTRICT
);
