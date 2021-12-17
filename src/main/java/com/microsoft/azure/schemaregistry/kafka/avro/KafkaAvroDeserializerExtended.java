// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.schemaregistry.kafka.avro;

import com.azure.core.util.serializer.TypeReference;
import com.azure.data.schemaregistry.SchemaRegistryClientBuilder;
import com.azure.data.schemaregistry.avro.SchemaRegistryAvroSerializer;
import com.azure.data.schemaregistry.avro.SchemaRegistryAvroSerializerBuilder;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.util.Map;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

import org.jboss.logging.Logger;

/**
 * Deserializer implementation for Kafka consumer, implementing Kafka Deserializer interface.
 *
 * Byte arrays are converted into Java objects by using the schema referenced by GUID prefix to deserialize the payload.
 *
 * Receiving Avro GenericRecords and SpecificRecords is supported.  Avro reflection capabilities have been disabled on
 * com.azure.schemaregistry.kafka.KafkaAvroSerializer.
 *
 * @see KafkaAvroSerializer See serializer class for upstream serializer implementation
 */
public class KafkaAvroDeserializerExtended implements Deserializer<Object> {
    private SchemaRegistryAvroSerializer serializer;

    /**
     * Empty constructor used by Kafka consumer
     */
    public KafkaAvroDeserializerExtended() {
        super();
    }

    /**
     * Configures deserializer instance.
     *
     * @param props Map of properties used to configure instance
     * @param isKey Indicates if deserializing record key or value.  Required by Kafka deserializer interface,
     *              no specific functionality has been implemented for key use.
     *
     * @see KafkaAvroDeserializerConfig Deserializer will use configs found in here and inherited classes.
     */
    public void configure(Map<String, ?> props, boolean isKey) {

        Boolean avroSpecificReader = KafkaAvroDeserializerConfig.AVRO_SPECIFIC_READER_CONFIG_DEFAULT;
        if (props.containsKey(KafkaAvroDeserializerConfig.AVRO_SPECIFIC_READER_CONFIG)) {
            avroSpecificReader = Boolean.valueOf( "" + props.get(KafkaAvroDeserializerConfig.AVRO_SPECIFIC_READER_CONFIG));
        }

        KafkaAvroDeserializerConfig config = new KafkaAvroDeserializerConfig((Map<String, Object>) props);

        String tenantId = ConfigProvider.getConfig().getValue("eh.tenant.id", String.class);
        String clientId = ConfigProvider.getConfig().getValue("eh.client.id", String.class);
        String clientSecret = ConfigProvider.getConfig().getValue("eh.client.secret", String.class);

        TokenCredential credential = new ClientSecretCredentialBuilder()
            .tenantId(tenantId)
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build();

        this.serializer = new SchemaRegistryAvroSerializerBuilder()
                .schemaRegistryAsyncClient(new SchemaRegistryClientBuilder()
                        .endpoint(config.getSchemaRegistryUrl())
                        //.credential(config.getCredential())
                        .credential(credential)
                        .maxCacheSize(config.getMaxSchemaMapSize())
                        .buildAsyncClient())
                //.avroSpecificReader(config.getAvroSpecificReader())
                .avroSpecificReader(avroSpecificReader)
                .buildSerializer();
    }

    /**
     * Deserializes byte array into Java object
     * @param topic topic associated with the record bytes
     * @param bytes serialized bytes, may be null
     * @return deserialize object, may be null
     */
    @Override
    public Object deserialize(String topic, byte[] bytes) {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return serializer.deserialize(in, TypeReference.createInstance(Object.class));
    }

    @Override
    public void close() { }
}
