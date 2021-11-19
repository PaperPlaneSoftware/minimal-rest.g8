BEGIN;

DROP TABLE IF EXISTS todo CASCADE;
CREATE TABLE todo (
    task TEXT NOT NULL,
    done BOOLEAN NOT NULL,
    created_by INT NOT NULL REFERENCES $name$_user(id),
    assigned_to INT NOT NULL REFERENCES $name$_user(id),
    id SERIAL PRIMARY KEY
);
GRANT INSERT, SELECT, UPDATE ON TABLE todo TO $name$_worker;

-- users can only insert todos under their own account
CREATE POLICY insert_own ON todo 
    FOR INSERT 
    TO $name$_worker 
    WITH CHECK (current_setting('auth.id')::INT = created_by);

-- users can only update todos they created or were assigned to
CREATE POLICY updated_created_or_assigned ON todo
    FOR UPDATE
    TO $name$_worker
    WITH CHECK ( 
        current_setting('auth.id')::INT = created_by OR 
        current_setting('auth.id')::INT = assigned_to
    );

-- users can only select todos they created or were assigned to
CREATE POLICY select_created_or_assigned ON todo
    FOR SELECT
    TO $name$_worker
    USING (
        current_setting('auth.id')::INT = created_by OR 
        current_setting('auth.id')::INT = assigned_to
    );

ALTER TABLE todo ENABLE ROW LEVEL SECURITY;

COMMIT;