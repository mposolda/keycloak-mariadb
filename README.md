# keycloak-mariadb

In the example, we will start 2 MariaDB Galera cluster nodes communicating with each other. Each node will run in separate docker container.

Then we will setup Keycloak cluster with 2 nodes, when Keycloak `node1` will use database on `mariadb-node1` and Keycloak `node2` will use database
on `mariadb-node2`. Change anything in admin console of Keycloak `node1`, you will be immediatelly able to see the changes on keycloak `node2` too, because
MariaDB databases are in cluster and use multi-master synchronous replication. 

## Building docker image
 
This is optional step, because there is already existing docker image `mposolda/mariadb-cluster`, so you can just pull this image instead of
building your own. So if you really follow this step and build the image by yourself, then replace all future occurences in next steps and use your
image `mariadb-cluster-image` instead of `mposolda/mariadb-cluster` image.

So building image is with these command:

```
cd keycloak-mariadb/dockerimage
docker build -t mariadb-cluster-image .
```

## Setup 2 nodes in MariaDB cluster

1) Install docker of version 1.10 or later (because of `docker network` command to be available). Follow documentation of your OS on how to do it.


2) Create separate docker bridge network to ensure that nodes see each other through embedded DNS provided by docker.

```
docker network create --driver bridge mariadb_cluster
```

3) Run MariaDB cluster node1 with those commands:

```
cd keycloak-mariadb;
export KEYCLOAK_MARIADB_PATH=$(pwd);
docker run --name mariadb-node1 --net=mariadb_cluster --net-alias=docker-mariadb-node1 \
-v $KEYCLOAK_MARIADB_PATH/mariadb-conf-volume:/etc/mysql/conf.d -e MYSQL_ROOT_PASSWORD=root \
-e MYSQL_INITDB_SKIP_TZINFO=foo -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak \
-d mposolda/mariadb-cluster:10.1 --wsrep-new-cluster;
```
 
You can check the progress of MariaDB cluster initialization by running

```
docker logs mariadb-node1
```


4) Run command to see what's the IP address of underlying docker container `mariadb-node1` :

```
docker inspect --format '{{ .NetworkSettings.Networks.mariadb_cluster.IPAddress }}' mariadb-node1;
```

Then you can change your `/etc/hosts` and add (or update) the entry for `docker-mariadb-node1` host (will be used later for datasource configuration).
Replace the IP with the real IP from previous `docker inspect` command.

```
172.19.0.2 docker-mariadb-node1
```


5) Run MariaDB cluster node2 with those commands:

```
cd keycloak-mariadb;
export KEYCLOAK_MARIADB_PATH=$(pwd);
docker run --name mariadb-node2 --net=mariadb_cluster --net-alias=docker-mariadb-node2 \
-v $KEYCLOAK_MARIADB_PATH/mariadb-conf-volume:/etc/mysql/conf.d -e MYSQL_ROOT_PASSWORD=root \
-e MYSQL_INITDB_SKIP_TZINFO=foo -d mposolda/mariadb-cluster:10.1 --wsrep_cluster_address=gcomm://docker-mariadb-node1;
```

You can check the progress of MariaDB cluster initialization by running

```
docker logs mariadb-node2
```


6) Run command to see what's the IP address of underlying docker container `mariadb-node2` :

```
docker inspect --format '{{ .NetworkSettings.Networks.mariadb_cluster.IPAddress }}' mariadb-node2;
```
   
Then you can change your `/etc/hosts` and add (or update) the entry for `docker-mariadb-node2` host (will be used later for datasource configuration).
Replace the IP with the real IP from previous `docker inspect` command.
   
```
172.19.0.3 docker-mariadb-node2
```


7) If you have MySQL client, you can try to connect to `docker-mariadb-node2` host and see if you are able to connect as "keycloak" user and see "keycloak" database.

```
mysql -h docker-mariadb-node2 -u keycloak -pkeycloak --execute="show databases";
```

The output should be like:

```
+--------------------+
| Database           |
+--------------------+
| information_schema |
| keycloak           |
+--------------------+
```


## Configure Keycloak cluster

So at this moment, we have 2 MariaDB galera cluster nodes on "docker-mariadb-node1" and "docker-mariadb-node2" . Next step is to 
 configure 2 Keycloak nodes when node1 will use "docker-mariadb-node1" and node2 will use "docker-mariadb-node2". 