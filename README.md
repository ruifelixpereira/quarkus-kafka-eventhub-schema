# quarkus-azure-kafka-demo Project

This project shows a Quarkus Kafka Avro scenario using Azure Event Hubs as a Kafka broker and Schema Registry.

## Setup Azure Event Hubs namespace

- Step 1. Create a new service principal to use in your appliction to access the Azure Event Hubs
- Step 2. Create new Azure Event Hub namespace
- Step 3. Create a new schema group in the Schema Registry
- Step 4. In the Event Hub namespace, assign the roles "Owner" and "Schema Registry Contributor" to you service principal.

## Compile and package the application

```shell script
mvn package -DskipTests
```

## Run the application

```shell script
java -jar ./target/quarkus-app/quarkus-run.jar
```

## Test it

While running the applcaition, open 2 new terminal windows.

In the first terminal, just run:

```shell script
curl -N http://localhost:8080/consumed-movies
```

In the second terminal, just post a new movie:

```shell script
curl --header "Content-Type: application/json" \
  --request POST \
  --data '{"title":"The Shawshank Redemption","year":1994}' \
  http://localhost:8080/movies
```