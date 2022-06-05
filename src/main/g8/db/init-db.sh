#!/bin/bash
set -e

POSTGRES="psql -U $POSTGRES_USER -d $POSTGRES_DB"

$POSTGRES <<-EOSQL
CREATE USER $POSTGRES_WORKER WITH LOGIN PASSWORD '$POSTGRES_WORKER_PASSWORD' ;
EOSQL