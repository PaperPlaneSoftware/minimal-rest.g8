#!/bin/bash
set -e

POSTGRES="psql -U $name$_owner -d $name$"

echo "Creating database role: \$POSTGRES_WORKER"

\$POSTGRES <<-EOSQL
CREATE USER $name$_worker WITH LOGIN PASSWORD '\$POSTGRES_WORKER_PASSWORD';
EOSQL