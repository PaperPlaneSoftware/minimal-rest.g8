-- Enter migration here
CREATE EXTENSION "pgcrypto";
CREATE EXTENSION "ltree";
CREATE EXTENSION "uuid-ossp";

CREATE TABLE tp_user (
    username TEXT NOT NULL,
    passwd TEXT NOT NULL,
    id SERIAL PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE tp_user TO trigpoint_worker;
GRANT USAGE, SELECT ON SEQUENCE tp_user_id_seq TO trigpoint_worker;

CREATE TABLE tp_user_session (
    tp_user_id INT NOT NULL REFERENCES tp_user(id),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    session_id UUID PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE tp_user_session TO trigpoint_worker;

CREATE TABLE todo (
    task TEXT NOT NULL,
    done BOOLEAN NOT NULL,
    created_by INT NOT NULL REFERENCES tp_user(id),
    id SERIAL PRIMARY KEY
);
GRANT INSERT, SELECT, UPDATE ON TABLE todo TO trigpoint_worker;

-- tp_users can only insert todos under their own account
CREATE POLICY insert_own ON todo 
    FOR INSERT 
    TO trigpoint_worker 
    WITH CHECK (current_setting('auth.id')::INT = created_by);

-- tp_users can only update todos they created or were assigned to
CREATE POLICY updated_created_or_assigned ON todo
    FOR UPDATE
    TO trigpoint_worker
    WITH CHECK (current_setting('auth.id')::INT = created_by);

-- tp_users can only select todos they created or were assigned to
CREATE POLICY select_created_or_assigned ON todo
    FOR SELECT
    TO trigpoint_worker
    USING (current_setting('auth.id')::INT = created_by);

ALTER TABLE todo ENABLE ROW LEVEL SECURITY;
