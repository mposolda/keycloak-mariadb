#!/bin/bash

echo "docker-entrypoint-replaced.sh: Executed";

if [ "$1" == "--wsrep-new-cluster" ]; then
  echo "docker-entrypoint-replaced.sh: Cluster startup requested.";
  /docker-entrypoint.sh "$@";
else
  echo "docker-entrypoint-replaced.sh: Cluster already started. Running docker-entrypoint-slave.sh";
  /docker-entrypoint-slave.sh "$@";
fi

## Increasing sleep time in the "docker-entrypoint.sh" script
#cat /docker-entrypoint.sh | sed s/sleep\ 1/sleep\ 9/ > /foo.txt
#mv /foo.txt /docker-entrypoint.sh
#chmod -v +x /docker-entrypoint.sh

#echo "docker-entrypoint-replaced.sh: Executing real entry point docker-entrypoint.sh with params: $@";

#/docker-entrypoint.sh "$@";

#echo "docker-entrypoint-replaced.sh: Finished execution";