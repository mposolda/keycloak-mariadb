FROM mariadb:10.1

RUN mkdir /keycloak-work
ADD docker-entrypoint-replaced.sh /docker-entrypoint-replaced.sh
ADD docker-entrypoint-slave.sh /docker-entrypoint-slave.sh

RUN chmod -v +x /docker-entrypoint-replaced.sh
RUN chmod -v +x /docker-entrypoint-slave.sh

ENTRYPOINT ["/docker-entrypoint-replaced.sh"]