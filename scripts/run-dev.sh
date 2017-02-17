#!/usr/bin/env bash
BASE_DIR=${BASE_DIR:-"$( cd "$( dirname "${BASH_SOURCE[0]}" )/.." && pwd )"}
$BASE_DIR/target/dist/bin/run.sh
