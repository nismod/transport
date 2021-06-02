#!/usr/bin/env bash
set -e
set -x

echo "Start dafni-run.sh"

# Run from data directory
pushd /data

    # Check inputs
    ls -lah ./inputs/*/*

    # Run model
    java -cp /root/transport.jar nismod.transport.App $ARGS

popd

echo "End dafni-run.sh"
