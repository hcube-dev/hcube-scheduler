#!/usr/bin/env bash
BASE_DIR=${BASE_DIR:-"$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"}

LOGBACK_FILE=${LOGBACK_FILE:-$BASE_DIR/conf/logback.xml}
DEBUG_PORT=${DEBUG_PORT:-5000}
DEBUG_SUSPEND=${DEBUG_SUSPEND:-n}

CLASSPATH="$BASE_DIR/lib/*"

set -x
exec java -Xmx1g \
    -cp "$CLASSPATH" \
    -Dlogback.configurationFile=$LOGBACK_FILE \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=$DEBUG_SUSPEND,address=$DEBUG_PORT \
    "$@" \
    hcube.scheduler.Boot
