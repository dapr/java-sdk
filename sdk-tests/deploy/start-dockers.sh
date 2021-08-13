#!/bin/sh
docker-compose -f local-test-kafka.yml -p java-sdk-kafka up -d
docker-compose -f local-test-vault.yml -p java-sdk-vault up -d
