#!/usr/bin/env bash
BASE_DIR=${BASE_DIR:-"$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"}

LOGBACK_FILE=${LOGBACK_FILE:-$BASE_DIR/conf/logback.xml}
CONF_FILE=${CONF_FILE:-file://$BASE_DIR/conf/scheduler.conf}
DEBUG_PORT=${DEBUG_PORT:-5000}
DEBUG_SUSPEND=${DEBUG_SUSPEND:-n}

CLASSPATH="$BASE_DIR/lib/*"

if [ ! -z $DEBUG ]
then
    DEBUG_AGENT="-agentlib:jdwp=transport=dt_socket,server=y,suspend=$DEBUG_SUSPEND,address=$DEBUG_PORT"
fi

set -x
exec java -Xmx1g \
    -cp "$CLASSPATH" \
    -Dlogback.configurationFile=$LOGBACK_FILE \
    -Dhcube.scheduler.conf-file=$CONF_FILE \
    $DEBUG_AGENT \
    "$@" \
    hcube.scheduler.Boot
