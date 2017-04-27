#!/usr/bin/env bash
version=${1:-latest}
image=hcube-scheduler:$version
docker build -t $image -f docker/Dockerfile .
