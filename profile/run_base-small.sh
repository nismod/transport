#!/usr/bin/env bash
script_dir=`dirname "$0"`

cd $script_dir/../transport && java \
  -XX:+UnlockCommercialFeatures \
  -XX:+FlightRecorder \
  -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,settings=../profile/settings-small.jfc \
  -XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints \
  -XX:MaxHeapSize=1g \
  -cp ./target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  nismod.transport.App \
  -c ./src/test/config/testConfig.properties \
  -b
