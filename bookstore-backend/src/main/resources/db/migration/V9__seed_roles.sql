-- Reference data: the application cannot assign a role that doesn't exist.
-- This runs in every environment, including production.
INSERT INTO roles (name) VALUES ('ROLE_CUSTOMER');
INSERT INTO roles (name) VALUES ('ROLE_ADMIN');
