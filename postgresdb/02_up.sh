#!/bin/bash

docker run --env-file ./env.list -p 5432:5432 -d --name game-library-postgresql dev/game-library-postgresql