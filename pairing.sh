#!/bin/bash
source .commons

docker-compose stop
initTemp true
docker-compose up
