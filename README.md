# keycloak-mariadb
How to easily set MariaDB cluster and configure with Keycloak

1) Build image (non-mandatory):

```
cd keycloak-mariadb/dockerimage
docker build -t mariadb-cluster .
```

(Or use the image mposolda/mariadb-cluster:10.1 )


2) 

docker network create --driver bridge mariadb_cluster


3) 
```
cd keycloak-mariadb;
export KEYCLOAK_MARIADB_PATH=$(pwd);
docker run --name mariadb-node1 --net=mariadb_cluster --net-alias=docker-mariadb-node1 -v $KEYCLOAK_MARIADB_PATH/mariadb-conf-volume:/etc/mysql/conf.d \ 
-e MYSQL_ROOT_PASSWORD=root -e MYSQL_INITDB_SKIP_TZINFO=foo -e MYSQL_DATABASE=keycloak -e MYSQL_USER=keycloak -e MYSQL_PASSWORD=keycloak \
-d mposolda/mariadb-cluster:10.1 --wsrep-new-cluster;
```
 
You can check the progress of MariaDB cluster initialization by running

docker logs mariadb-node1


4) Run command to see what's the IP address of underlying docker container.
docker inspect --format '{{ .NetworkSettings.Networks.mariadb_cluster.IPAddress }}' mariadb-node1;

Then you can change your "/etc/hosts" and add (or update) the entry for "docker-mariadb-node1" host (will be used later for datasource configuration)

172.19.0.2 docker-mariadb-node1


5) Start the second mariadb cluster node

docker run --name mariadb-node2 --net=mariadb_cluster --net-alias=docker-mariadb-node2 -v $KEYCLOAK_MARIADB_PATH/mariadb-conf-volume:/etc/mysql/conf.d 
-e MYSQL_ROOT_PASSWORD=root -e MYSQL_INITDB_SKIP_TZINFO=foo -d mposolda/mariadb-cluster:10.1 --wsrep_cluster_address=gcomm://docker-mariadb-node1;

You can check the progress of MariaDB cluster initialization by running

docker logs mariadb-node2


6) Run command to see what's the IP address of underlying docker container.
docker inspect --format '{{ .NetworkSettings.Networks.mariadb_cluster.IPAddress }}' mariadb-node2;
   
Then you can change your "/etc/hosts" and add (or update) the entry for "docker-mariadb-node2" host (will be used later for datasource configuration)
   
172.19.0.3 docker-mariadb-node2


7) If you have MySQL client, you can try to connect to "docker-mariadb-node2" host and see if you are able to connect as "keycloak" user and see "keycloak" database.

mysql -h docker-mariadb-node2 -u keycloak -pkeycloak --execute="show databases";

The output should be like:

+--------------------+
| Database           |
+--------------------+
| information_schema |
| keycloak           |
+--------------------+


## Configure Keycloak cluster

So at this moment, we have 2 MariaDB galera cluster nodes on "docker-mariadb-node1" and "docker-mariadb-node2" . Next step is to 
 configure 2 Keycloak nodes when node1 will use "docker-mariadb-node1" and node2 will use "docker-mariadb-node2". 