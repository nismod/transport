#!/usr/bin/env bash
script_dir=`dirname "$0"`

cd $script_dir/../transport && java \
  -XX:+UnlockCommercialFeatures \
  -XX:+FlightRecorder \
  -XX:FlightRecorderOptions=defaultrecording=true,dumponexit=true,settings=../profile/settings.jfc \
  -XX:MaxHeapSize=120g \
  -cp ./target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar \
  nismod.transport.App \
  -c ./src/main/full/config/config.properties \
  -r 2020 2015
