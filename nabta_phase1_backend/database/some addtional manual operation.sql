use nabta_db;
DESCRIBE products;
DESCRIBE customer_orders;
SELECT id, name, name_ar, price, stock_quantity, category_id FROM products LIMIT 10;
SHOW FUNCTION STATUS WHERE Db = 'nabta_db';
DESCRIBE order_items;
DELIMITER //
CREATE FUNCTION fn_order_subtotal(p_order_id BIGINT)
RETURNS DECIMAL(12,2)
NOT DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_total DECIMAL(12,2) DEFAULT 0.00;
    SELECT COALESCE(SUM(line_total), 0.00)
    INTO v_total
    FROM order_items
    WHERE order_id = p_order_id;
    RETURN v_total;
END//
DELIMITER ;

SELECT id, order_number, subtotal, total_amount
FROM customer_orders
ORDER BY id DESC
LIMIT 10;

SELECT fn_order_subtotal(13) AS subtotal_sar;

SHOW PROCEDURE STATUS WHERE Db = 'nabta_db';

USE nabta_db;

DELIMITER //

CREATE PROCEDURE sp_order_status_summary()
BEGIN
    SELECT status, COUNT(*) AS order_count
    FROM customer_orders
    GROUP BY status
    ORDER BY status;
END//

DELIMITER ;

CALL sp_order_status_summary();

SHOW TRIGGERS FROM nabta_db;

DESCRIBE order_status_history;

USE nabta_db;

DELIMITER //

CREATE TRIGGER trg_customer_order_status_history_after_update
AFTER UPDATE ON customer_orders
FOR EACH ROW
BEGIN
    IF NOT (OLD.status <=> NEW.status) THEN
        INSERT INTO order_status_history
            (order_id, actor_user_id, previous_status,
             new_status, note, created_at)
        VALUES
            (NEW.id, NULL, OLD.status, NEW.status,
             CONCAT('Status changed from ', OLD.status,
                    ' to ', NEW.status),
             CURRENT_TIMESTAMP(6));
    END IF;
END//

DELIMITER ;

SHOW TRIGGERS FROM nabta_db;
DESCRIBE users;
SHOW CREATE TABLE customer_orders;

SELECT id, name, role, status FROM users ORDER BY id;

INSERT INTO customer_orders
(contact_phone, created_at, delivery_address, delivery_charge,
 idempotency_key, order_notes, order_number, payment_method,
 status, subtotal, total_amount, updated_at, user_id) VALUES
('0000000', CURRENT_TIMESTAMP(6), 'TEST ONLY - DO NOT DELIVER',  0.00, NULL, 'TEST ORDER FOR DB-06 ONLY',
 'TEST-TRIGGER-20261020-01', 'CASH_ON_DELIVERY', 'PENDING', 1.00, 1.00, CURRENT_TIMESTAMP(6), 4);
 
 SELECT id, order_number, status FROM customer_orders WHERE order_number = 'TEST-TRIGGER-20261020-01';
 
 UPDATE customer_orders SET status = 'PROCESSING', updated_at = CURRENT_TIMESTAMP(6)
WHERE id = 14  AND order_number = 'TEST-TRIGGER-20261020-01'  AND status = 'PENDING';

SELECT id, order_id, previous_status, new_status, actor_user_id, note, created_at
FROM order_status_history WHERE order_id = 14 ORDER BY created_at DESC;