# quarkus-azure-kafka-demo Project

This project shows a Quarkus Kafka Avro scenario using Azure Event Hubs as a Kafka broker and Schema Registry.
It was built on top of the Quarkus example described [here](https://quarkus.io/guides/kafka-schema-registry-avro).

## Setup Azure Event Hubs namespace

- Step 1. Create a new service principal to use in your application to access the Azure Event Hubs
- Step 2. Create new Azure Event Hub namespace
- Step 3. Create a new schema group in the Schema Registry
- Step 4. In the Event Hub namespace, assign the roles "Owner" and "Schema Registry Contributor" to you service principal.

## Setup your environemnt

Go to `src/main/resources/application.properties` and replace the following values with your specific environment settings:
- `eh.namespace=[your-event-hubs-namespace]`
- `kafka.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="$ConnectionString" password="[your connection string]";`
- `eh.tenant.id=[tenant-id]`
- `eh.client.id=[client-id]`
- `eh.client.secret=[client-secret]`

## Compile and package the application

```shell script
mvn package -DskipTests
```

## Run the application

```shell script
java -jar ./target/quarkus-app/quarkus-run.jar
```

## Test it

While running the application, open 2 new terminal windows.

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

