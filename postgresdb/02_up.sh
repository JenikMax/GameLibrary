#!/bin/bash

# shellcheck disable=SC2046
export $(grep -v '^#' ../.env | xargs) && \
docker run -e POSTGRES_PASSWORD -e DB_PASSWORD -p 5432:5432 -d --name game-library-postgresql dev/game-library-postgresql