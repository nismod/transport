# Profiling

This directory contains scripts that profile the model's performance.

First build the model jar to `target/transport-0.0.1-SNAPSHOT-jar-with-dependencies.jar`, then
run one or more of the scripts to produce profile outputs as Java Flight Recordings as `.jfr`
files:

- `run_base-small.sh` and `run_predict-small.sh` profile the model running for the base year
  and the first predicted year using the test data specified by
  `src/test/config/testConfig.properties`
- `run_base.sh` and `run_predict.sh` profile the model running for the base year and the first
  predicted year using a larger dataset, which should be downloaded and copied to
  `src/main/full`, with details specified by `src/main/full/config/config.properties`


Run Java Mission Control (normally found at `$JAVA_HOME/bin/jmc.exe`) then `File > Open` to
open a `.jfr` file and view the results.

The `Code > Call Tree` tab lets you drill through time spent in each method (actually number
of times that the method appeared in a sample).

The `Memory > Object Statistics` tab indicates which objects were using memory at each point
when the process was sampled.

The Java documentation gives more information on [how to produce a flight
recording](https://docs.oracle.com/javase/8/docs/technotes/guides/troubleshoot/tooldescr004.html#BABHCDEA)
and the command-line flags available.
