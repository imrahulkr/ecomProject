-- roles is Hibernate-managed (ddl-auto=update created the table from the Role entity), but
-- nothing has ever seeded rows into it - AuthServiceImpl.register() and the seller-approval flow
-- both look roles up by name and fail outright if they're missing.
INSERT INTO roles (role_name)
SELECT r FROM (VALUES ('ROLE_USER'), ('ROLE_SELLER'), ('ROLE_ADMIN')) AS v(r)
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE role_name = v.r);
