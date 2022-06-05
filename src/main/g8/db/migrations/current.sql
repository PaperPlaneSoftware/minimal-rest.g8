CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DROP TABLE IF EXISTS user_session;
DROP TABLE IF EXISTS company;
DROP TABLE IF EXISTS app_user;

-- users
-- nb: creation by an individual (UI)
-- nb: uniqueness assumed by checking {email}
CREATE TABLE IF NOT EXISTS app_user (
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
	firstName TEXT NOT NULL, 
	lastName TEXT NOT NULL, 
    email TEXT NOT NULL,
    passwd TEXT NOT NULL,
    id SERIAL PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE app_user TO $name;format="space,snake"$_worker; -- GRANT TABLE LEVEL PERMISSIONS
GRANT USAGE, SELECT ON SEQUENCE app_user_id_seq TO $name;format="space,snake"$_worker; -- SAME AS ABOVE BUT FOR A SEQUENCE

-- companies
-- nb: creation by users (UI) during onboarding
-- nb: uniqueness assumed by checking {company_name, postcode}
CREATE TABLE IF NOT EXISTS company (
	created_at TIMESTAMPTZ NOT NULL, 
	updated_at TIMESTAMPTZ NOT NULL,
	created_by_user_id INTEGER REFERENCES app_user(id),
	company_address TEXT,
	company_name TEXT NOT NULL,
	postcode VARCHAR(8) NOT NULL,
	id SERIAL PRIMARY KEY
);
GRANT INSERT, SELECT, UPDATE ON TABLE company TO $name;format="space,snake"$_worker;
GRANT USAGE, SELECT ON SEQUENCE company_id_seq TO $name;format="space,snake"$_worker;

CREATE POLICY select_own ON company
    FOR SELECT
    TO $name;format="space,snake"$_worker
    USING (current_setting('auth.id')::INT = created_by_user_id);

ALTER TABLE company ENABLE ROW LEVEL SECURITY;
COMMENT ON TABLE company IS 'A company e.g. a client or sub-contractor';

-- user sessions
CREATE TABLE IF NOT EXISTS user_session (
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    user_id INT NOT NULL REFERENCES app_user(id),
    session_id UUID PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE user_session TO $name;format="space,snake"$_worker;

/*** STORED PROCS (FOR DEV MOCK FIXTURES) ***/
CREATE OR REPLACE FUNCTION get_user_id(e_mail TEXT) RETURNS INTEGER
AS
\$\$
	SELECT id as user_id FROM app_user WHERE email = e_mail;
\$\$
LANGUAGE SQL;

CREATE OR REPLACE FUNCTION get_company_id(comp_name TEXT, p_code VARCHAR(8)) RETURNS INTEGER
AS
\$\$
	SELECT id as company_id FROM company WHERE company_name = comp_name and postcode = p_code;
\$\$
LANGUAGE SQL;