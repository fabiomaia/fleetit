#! /bin/bash
docker exec -it broker_postgres_1 psql -h localhost -p 5432 -U root -d fleetit -c "select * from updates;"
