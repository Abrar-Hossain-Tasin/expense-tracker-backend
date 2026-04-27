INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (name) VALUES ('ROLE_USER');

INSERT INTO users (username, email, password_hash, rent_share, is_active, role_id, created_at)
VALUES (
  'admin',
  'admin@house.com',
  '$2a$10$Pjp2XpPQNRCOb2TteHBAVOf.U1541ZTtjNxTYtGvam1VIGJMhr6Pe',
  0,
  TRUE,
  (SELECT id FROM roles WHERE name = 'ROLE_ADMIN'),
  CURRENT_TIMESTAMP
);

INSERT INTO users (username, email, password_hash, rent_share, is_active, role_id, created_at)
VALUES (
  'maya',
  'maya@house.com',
  '$2a$10$Pjp2XpPQNRCOb2TteHBAVOf.U1541ZTtjNxTYtGvam1VIGJMhr6Pe',
  420,
  TRUE,
  (SELECT id FROM roles WHERE name = 'ROLE_USER'),
  CURRENT_TIMESTAMP
);

INSERT INTO users (username, email, password_hash, rent_share, is_active, role_id, created_at)
VALUES (
  'niko',
  'niko@house.com',
  '$2a$10$Pjp2XpPQNRCOb2TteHBAVOf.U1541ZTtjNxTYtGvam1VIGJMhr6Pe',
  410,
  TRUE,
  (SELECT id FROM roles WHERE name = 'ROLE_USER'),
  CURRENT_TIMESTAMP
);

INSERT INTO users (username, email, password_hash, rent_share, is_active, role_id, created_at)
VALUES (
  'sara',
  'sara@house.com',
  '$2a$10$Pjp2XpPQNRCOb2TteHBAVOf.U1541ZTtjNxTYtGvam1VIGJMhr6Pe',
  430,
  TRUE,
  (SELECT id FROM roles WHERE name = 'ROLE_USER'),
  CURRENT_TIMESTAMP
);

INSERT INTO grocery_purchases (payer_id, amount, purchase_date, description, month_key)
VALUES (
  (SELECT id FROM users WHERE username = 'admin'),
  85.50,
  DATE '2026-04-05',
  'Pantry reset and breakfast staples',
  '2026-04'
);

INSERT INTO grocery_purchases (payer_id, amount, purchase_date, description, month_key)
VALUES (
  (SELECT id FROM users WHERE username = 'maya'),
  54.20,
  DATE '2026-04-12',
  'Produce, yogurt, and tea',
  '2026-04'
);

INSERT INTO grocery_purchases (payer_id, amount, purchase_date, description, month_key)
VALUES (
  (SELECT id FROM users WHERE username = 'niko'),
  71.00,
  DATE '2026-04-20',
  'Protein top-up and frozen meals',
  '2026-04'
);

INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'admin'), DATE '2026-04-20', 2);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'maya'), DATE '2026-04-20', 3);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'niko'), DATE '2026-04-20', 2);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'sara'), DATE '2026-04-20', 1);

INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'admin'), DATE '2026-04-21', 1);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'maya'), DATE '2026-04-21', 2);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'niko'), DATE '2026-04-21', 3);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'sara'), DATE '2026-04-21', 2);

INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'admin'), DATE '2026-04-22', 2);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'maya'), DATE '2026-04-22', 1);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'niko'), DATE '2026-04-22', 2);
INSERT INTO meal_entries (user_id, date, meals_count)
VALUES ((SELECT id FROM users WHERE username = 'sara'), DATE '2026-04-22', 3);

INSERT INTO house_expenses (title, category, total_amount, expense_date, split_type, is_recurring, created_by, created_at)
VALUES (
  'Electric bill',
  'Utilities',
  120.00,
  DATE '2026-04-10',
  'EQUAL',
  FALSE,
  (SELECT id FROM users WHERE username = 'admin'),
  CURRENT_TIMESTAMP
);

INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Electric bill'), (SELECT id FROM users WHERE username = 'admin'), 30.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Electric bill'), (SELECT id FROM users WHERE username = 'maya'), 30.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Electric bill'), (SELECT id FROM users WHERE username = 'niko'), 30.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Electric bill'), (SELECT id FROM users WHERE username = 'sara'), 30.00);

INSERT INTO house_expenses (title, category, total_amount, expense_date, split_type, is_recurring, created_by, created_at)
VALUES (
  'Streaming bundle',
  'Subscriptions',
  36.00,
  DATE '2026-04-14',
  'PERCENT',
  TRUE,
  (SELECT id FROM users WHERE username = 'maya'),
  CURRENT_TIMESTAMP
);

INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Streaming bundle'), (SELECT id FROM users WHERE username = 'admin'), 20.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Streaming bundle'), (SELECT id FROM users WHERE username = 'maya'), 40.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Streaming bundle'), (SELECT id FROM users WHERE username = 'niko'), 20.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Streaming bundle'), (SELECT id FROM users WHERE username = 'sara'), 20.00);

INSERT INTO house_expenses (title, category, total_amount, expense_date, split_type, is_recurring, created_by, created_at)
VALUES (
  'Cleaning supplies',
  'Home',
  48.00,
  DATE '2026-04-18',
  'RATIO',
  FALSE,
  (SELECT id FROM users WHERE username = 'niko'),
  CURRENT_TIMESTAMP
);

INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Cleaning supplies'), (SELECT id FROM users WHERE username = 'admin'), 2.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Cleaning supplies'), (SELECT id FROM users WHERE username = 'maya'), 1.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Cleaning supplies'), (SELECT id FROM users WHERE username = 'niko'), 1.00);
INSERT INTO expense_splits (expense_id, user_id, share_value)
VALUES ((SELECT id FROM house_expenses WHERE title = 'Cleaning supplies'), (SELECT id FROM users WHERE username = 'sara'), 1.00);
