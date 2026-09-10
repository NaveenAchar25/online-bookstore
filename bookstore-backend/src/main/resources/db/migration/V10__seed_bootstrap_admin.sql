-- Bootstrap admin account, provisioned via migration rather than the public
-- registration endpoint — mirrors how real systems avoid handing out
-- privileged access through a self-service signup form.
--
-- Password: ChangeMe@Admin123  (BCrypt hash below, cost factor 12)
-- must_change_password = TRUE forces rotation before this account should be
-- used for anything beyond an initial login in a real deployment.
INSERT INTO users (email, password_hash, first_name, last_name, must_change_password)
VALUES (
    'admin@bookstore.local',
    '$2b$12$m5bb.Q/BlLNTryuYBr7SXu2wQEV4T/nhwMKRnvngqLJJeQbsGkb4O',
    'System',
    'Administrator',
    TRUE
);

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u, roles r
WHERE u.email = 'admin@bookstore.local'
  AND r.name = 'ROLE_ADMIN';
