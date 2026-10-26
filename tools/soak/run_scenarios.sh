#!/bin/bash
# run_scenarios.sh <scenario> ... : run each scenario alone on the soak server (console.fifo), wait for its summary,
# print the result lines. SOAK_ROOT is the test folder (srv/ = the pack server, console.fifo = its stdin).
cd "$SOAK_ROOT"
for s in "$@"; do
  n=$(grep -c "Scenarios: " srv/logs/latest.log)
  echo "gregguns test $s" > console.fifo
  for i in $(seq 1 200); do sleep 3; [ "$(grep -c 'Scenarios: ' srv/logs/latest.log)" -gt "$n" ] && break; done
  tail -40 srv/logs/latest.log | grep -E "PASS $s|FAIL $s|SKIP $s|    |Scenarios" | tail -6 | cut -c1-700
done
