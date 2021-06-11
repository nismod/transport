#!/usr/bin/env bash
set -x

echo "$(date -I'seconds') Start dafni-run.sh" | tee -a /data/outputs/log.txt

# Run from data directory
pushd /data

    # Check inputs
    ls -lah ./inputs/*/* | tee -a /data/outputs/log.txt

    # Run model
    echo "$(date -I'seconds') java -cp /root/transport.jar nismod.transport.App $ARGS"  | tee -a /data/outputs/log.txt
    java -cp /root/transport.jar nismod.transport.App $ARGS | tee -a /data/outputs/log.txt

popd

echo "$(date -I'seconds') End dafni-run.sh" | tee -a /data/outputs/log.txt
