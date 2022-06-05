BEGIN;

DELETE FROM user_session;
DELETE FROM company;
DELETE FROM app_user;

INSERT INTO app_user(created_at, updated_at, firstName, lastName, email, passwd) VALUES (NOW(), NOW(), 'ad', 'min', 'admin@tp.co.uk', crypt('admin', gen_salt('bf')));
INSERT INTO app_user(created_at, updated_at, firstName, lastName, email, passwd) VALUES (NOW(), NOW(), 'adam', 'newman', 'adam@heron.co.uk', crypt('adam', gen_salt('bf')));
INSERT INTO app_user(created_at, updated_at, firstName, lastName, email, passwd) VALUES (NOW(), NOW(), 'oli', 'winks', 'oli@pp.co.uk', crypt('oli', gen_salt('bf')));
INSERT INTO app_user(created_at, updated_at, firstName, lastName, email, passwd) VALUES (NOW(), NOW(), 'chris', 'newman', 'chris@deeleys.co.uk', crypt('chris', gen_salt('bf')));

INSERT INTO company(created_at, updated_at, created_by_user_id, company_name, postcode) VALUES(NOW(), NOW(), get_user_id('chris@deeleys.co.uk'), 'Deeleys', 'CV6');
INSERT INTO company(created_at, updated_at, created_by_user_id, company_name, postcode) VALUES(NOW(), NOW(), get_user_id('adam@heron.co.uk'), 'Deeleys', 'BN6');
INSERT INTO company(created_at, updated_at, created_by_user_id, company_name, postcode) VALUES(NOW(), NOW(), get_user_id('adam@heron.co.uk'), 'Heron', 'BN6 9YH');
INSERT INTO company(created_at, updated_at, created_by_user_id, company_name, postcode) VALUES(NOW(), NOW(), get_user_id('oli@pp.co.uk'), 'Paper Plane', 'BN6 8JZ');

COMMIT;