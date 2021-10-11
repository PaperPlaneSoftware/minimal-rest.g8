#!/bin/bash
set -e

POSTGRES="psql -U $name;format="camel"$_owner -d $name;format="camel"$"


echo "Creating database role: \$POSTGRES_WORKER"

\$POSTGRES <<-EOSQL
CREATE USER $name;format="camel"$_worker WITH LOGIN PASSWORD '\$POSTGRES_WORKER_PASSWORD';
EOSQL