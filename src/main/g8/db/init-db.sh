#!/bin/bash
set -e

POSTGRES="psql -U $name;format="space,snake"$_owner -d $name$"

echo "Creating database role: \$POSTGRES_WORKER"

\$POSTGRES <<-EOSQL
CREATE USER $name;format="space,snake"$_worker WITH LOGIN PASSWORD '\$POSTGRES_WORKER_PASSWORD';
EOSQL