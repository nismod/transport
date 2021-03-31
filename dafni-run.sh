#!/usr/bin/env bash
set -e
set -x

# Run from data directory
cd /data

# Run model
java -cp /root/transport.jar nismod.transport.App $ARGS
