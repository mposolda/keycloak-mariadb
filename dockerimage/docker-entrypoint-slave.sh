#!/bin/bash
set -eo pipefail

# if command starts with an option, prepend mysqld
if [ "${1:0:1}" = '-' ]; then
	set -- mysqld "$@"
fi


# Get config
DATADIR="$("$@" --verbose --help --log-bin-index=`mktemp -u` 2>/dev/null | awk '$1 == "datadir" { print $2; exit }')"

echo "docker-entrypoint-slave.sh: datadir is $DATADIR";

if [ ! -d "$DATADIR/mysql" ]; then

		mkdir -p "$DATADIR"
		chown -R mysql:mysql "$DATADIR"

		echo 'Initializing database'
		mysql_install_db --user=mysql --datadir="$DATADIR" --rpm
		echo 'Database initialized'

fi

chown -R mysql:mysql "$DATADIR"

echo "docker-entrypoint-slave.sh: Executing command: $@";

exec "$@"