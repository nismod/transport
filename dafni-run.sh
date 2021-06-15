#!/usr/bin/env bash

# Create results directory if not exists
mkdir -p /data/inputs/results
# Copy results from inputs to outputs
# (outputs of previous steps turn up in inputs)
mv /data/inputs/results /data/outputs/results

echo "$(date -I'seconds') Start dafni-run.sh" |& tee -a /data/outputs/results/log.txt

free -g |& tee -a /data/outputs/results/log.txt

java $JAVA_OPTS -XshowSettings:vm -version |& tee -a /data/outputs/results/log.txt

# Run from data directory
pushd /data

    # Check inputs and outputs
    tree . |& tee -a /data/outputs/results/log.txt

    # Run model
    echo "$(date -I'seconds') java $JAVA_OPTS -cp /root/transport.jar nismod.transport.App $ARGS"  |& tee -a /data/outputs/results/log.txt
    java $JAVA_OPTS -cp /root/transport.jar nismod.transport.App $ARGS |& tee -a /data/outputs/results/log.txt

popd

echo "$(date -I'seconds') End dafni-run.sh" |& tee -a /data/outputs/results/log.txt
