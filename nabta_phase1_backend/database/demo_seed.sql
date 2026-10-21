-- OPTIONAL sample data for local UI integration. Run only after the backend has started once
-- (Hibernate creates the tables) and only if the tables are empty. Prices/stock are illustrative.
USE nabta_db;
INSERT IGNORE INTO categories (id, name, description, image_url, is_active) VALUES
(1,'Plants','Indoor and outdoor plants',NULL,1),
(2,'Pots','Pots and planters',NULL,1),
(3,'Care','Plant care essentials',NULL,1);
INSERT IGNORE INTO products (id,name,description,price,image_url,category_id,stock_quantity,is_active,created_at,updated_at) VALUES
(1,'Snake Plant','A hardy indoor plant for beginners',19.99,NULL,1,25,1,NOW(),NOW()),
(2,'Peace Lily','A leafy flowering houseplant',24.50,NULL,1,12,1,NOW(),NOW()),
(3,'Terracotta Pot','Classic terracotta planter',8.00,NULL,2,40,1,NOW(),NOW()),
(4,'Ceramic Planter','Glazed ceramic planter',16.75,NULL,2,18,1,NOW(),NOW()),
(5,'Potting Mix','General purpose potting mix',6.25,NULL,3,30,1,NOW(),NOW()),
(6,'Plant Spray Bottle','Reusable fine-mist spray bottle',4.50,NULL,3,5,1,NOW(),NOW());
