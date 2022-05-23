-- Enter migration here
CREATE IF NOT EXISTS EXTENSION "pgcrypto";
CREATE IF NOT EXISTS EXTENSION "ltree";
CREATE IF NOT EXISTS EXTENSION "uuid-ossp";

DROP TABLE IF EXISTS app_user;
DROP TABLE IF EXISTS user_session;
DROP TABLE IF EXISTS todo;

CREATE TABLE app_user (
    username TEXT NOT NULL,
    passwd TEXT NOT NULL,
    id SERIAL PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE app_user TO $name;format="space,snake"$_worker;
GRANT USAGE, SELECT ON SEQUENCE app_user_id_seq TO $name;format="space,snake"$_worker;

CREATE TABLE user_session (
    user_id INT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    session_id UUID PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE user_session TO $name;format="space,snake"$_worker;

CREATE TABLE todo (
    task TEXT NOT NULL,
    done BOOLEAN NOT NULL,
    created_by INT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    id SERIAL PRIMARY KEY
);
GRANT INSERT, SELECT, UPDATE ON TABLE todo TO $name;format="space,snake"$_worker;

-- tp_users can only insert todos under their own account
CREATE POLICY insert_own ON todo 
    FOR INSERT 
    TO $name;format="space,snake"$_worker 
    WITH CHECK (current_setting('auth.id')::INT = created_by);

-- tp_users can only update todos they created or were assigned to
CREATE POLICY updated_created_or_assigned ON todo
    FOR UPDATE
    TO $name;format="space,snake"$_worker
    WITH CHECK (current_setting('auth.id')::INT = created_by);

-- tp_users can only select todos they created or were assigned to
CREATE POLICY select_created_or_assigned ON todo
    FOR SELECT
    TO $name;format="space,snake"$_worker
    USING (current_setting('auth.id')::INT = created_by);

ALTER TABLE todo ENABLE ROW LEVEL SECURITY;
