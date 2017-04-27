#!/usr/bin/env bash
set -x

NET=${NET:-hcube}
NAME_PREFIX=${NAME_PREFIX:-hcube-scheduler}

# remove existing containers in given network
names=$(docker ps --filter "network=${NET}" --format "{{.Names}}" | grep ${NAME_PREFIX})
for name in ${names}; do
    docker rm -f ${name}
done
