#!/bin/bash
source .commons

docker-compose stop
permitJoin true
docker-compose up
