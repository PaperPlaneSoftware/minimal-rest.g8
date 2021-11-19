BEGIN;

INSERT INTO osi_user(username, passwd) 
VALUES 
    ('admin', crypt('admin', gen_salt('bf')));

COMMIT;