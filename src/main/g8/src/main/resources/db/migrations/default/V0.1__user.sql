BEGIN;

DROP TABLE IF EXISTS $name$_user;
CREATE TABLE $name$_user (
    username TEXT NOT NULL,
    passwd TEXT NOT NULL,
    id SERIAL PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE $name$_user TO $name$_worker;

DROP TABLE IF EXISTS user_session;
CREATE TABLE user_session (
    user_id INT NOT NULL REFERENCES $name$_user(id),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    session_id UUID PRIMARY KEY
);
GRANT SELECT, INSERT, DELETE, UPDATE ON TABLE user_session TO $name$_worker;

COMMIT;