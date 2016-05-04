#!/bin/bash

echo "docker-entrypoint-replaced.sh: Executed";

if [ "$1" == "--wsrep-new-cluster" ]; then
  echo "docker-entrypoint-replaced.sh: Cluster startup requested.";
  /docker-entrypoint.sh "$@";
else
  echo "docker-entrypoint-replaced.sh: Cluster already started. Running docker-entrypoint-slave.sh";
  /docker-entrypoint-slave.sh "$@";
fi
