#!/usr/bin/env bash
set -x
BASE_DIR=${BASE_DIR:-"$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"}

ETCD_SIZE=${ETCD_SIZE:-3}
SCHED_SIZE=${SCHED_SIZE:-3}
NET=${NET:-hcube}


# create network
docker network create --driver bridge ${NET}
if [ $? -ne 0 ]; then
    # remove existing containers
    i=0
    while [ ${i} -lt ${ETCD_SIZE} ]; do
        name=hcube-scheduler-etcd-${i}
        docker rm -f ${name}
        ((i++))
    done
    i=0
    while [ ${i} -lt ${SCHED_SIZE} ]; do
        name=hcube-scheduler-${i}
        docker rm -f ${name}
        ((i++))
    done
fi

# run etcd cluster
INITIAL_ETCD_CLUSTER_TOKEN="hcube-scheduler-etcd-token-${ETCD_SIZE}"
INITIAL_ETCD_CLUSTER=""
i=0
while [ ${i} -lt ${ETCD_SIZE} ]; do
    name=hcube-scheduler-etcd-${i}
    INITIAL_ETCD_CLUSTER+="$name=http://$name:2380,"
    ((i++))
done

i=0
while [ ${i} -lt ${ETCD_SIZE} ]; do
    name="hcube-scheduler-etcd-${i}"
    docker run \
        --net hcube \
        --name ${name} \
        --hostname ${name} \
        --restart=unless-stopped \
        -d quay.io/coreos/etcd:v3.1.0 \
        etcd \
            --name ${name} \
            --advertise-client-urls http://${name}:2379,http://${name}:4001 \
            --listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001 \
            --initial-advertise-peer-urls http://${name}:2380 \
            --listen-peer-urls http://0.0.0.0:2380 \
            --initial-cluster-token ${INITIAL_ETCD_CLUSTER_TOKEN} \
            --initial-cluster ${INITIAL_ETCD_CLUSTER} \
            --initial-cluster-state new \
            --log-package-levels '*=DEBUG'
    ((i++))
done

# rebuild scheduler
sbt clean dist

# build docker image
${BASE_DIR}/docker/docker-build.sh

# run scheduler cluster
i=0
while [ ${i} -lt ${SCHED_SIZE} ]; do
    name="hcube-scheduler-${i}"
    docker run \
        --net hcube \
        --name ${name} \
        --hostname ${name} \
        --restart=unless-stopped \
        -d hcube-scheduler:latest
    ((i++))
done