// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.schemaregistry.kafka.avro;

import com.azure.data.schemaregistry.SchemaRegistryClientBuilder;
import com.azure.data.schemaregistry.avro.SchemaRegistryAvroSerializer;
import com.azure.data.schemaregistry.avro.SchemaRegistryAvroSerializerBuilder;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * Serializer implementation for Kafka producer, implementing the Kafka Serializer interface.
 *
 * Objects are converted to byte arrays containing an Avro-encoded payload and is prefixed with a GUID pointing
 * to the matching Avro schema in Azure Schema Registry.
 *
 * Currently, sending Avro GenericRecords and SpecificRecords is supported.  Avro reflection has been disabled.
 *
 * @see KafkaAvroDeserializer See deserializer class for downstream deserializer implementation
 */
public class KafkaAvroSerializerExtended implements Serializer<Object> {
    private SchemaRegistryAvroSerializer serializer;

    /**
     * Empty constructor for Kafka producer
     */
    public KafkaAvroSerializerExtended() {
        super();
    }

    /**
     * Configures serializer instance.
     *
     * @param props Map of properties used to configure instance.
     * @param isKey Indicates if serializing record key or value.  Required by Kafka serializer interface,
     *              no specific functionality implemented for key use.
     *
     * @see KafkaAvroSerializerConfig Serializer will use configs found in KafkaAvroSerializerConfig.
     */
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {

        Boolean autoRegisterSchemas = KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS_CONFIG_DEFAULT;
        if (props.containsKey(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS_CONFIG)) {
            autoRegisterSchemas = Boolean.valueOf( "" + props.get(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS_CONFIG));
        }

        KafkaAvroSerializerConfig config = new KafkaAvroSerializerConfig((Map<String, Object>) props);

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
                .schemaGroup(config.getSchemaGroup())
                //.autoRegisterSchema(config.getAutoRegisterSchemas())
                .autoRegisterSchema(autoRegisterSchemas)
                .buildSerializer();
    }


    /**
     * Serializes GenericRecord or SpecificRecord into a byte array, containing a GUID reference to schema
     * and the encoded payload.
     *
     * Null behavior matches Kafka treatment of null values.
     *
     * @param topic Topic destination for record. Required by Kafka serializer interface, currently not used.
     * @param record Object to be serialized, may be null
     * @return byte[] payload for sending to EH Kafka service, may be null
     * @throws SerializationException Exception catchable by core Kafka producer code
     */
    @Override
    public byte[] serialize(String topic, Object record) {
        // null needs to treated specially since the client most likely just wants to send
        // an individual null value instead of making the subject a null type. Also, null in
        // Kafka has a special meaning for deletion in a topic with the compact retention policy.
        // Therefore, we will bypass schema registration and return a null value in Kafka, instead
        // of an Avro encoded null.
        if (record == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        serializer.serialize(out, record);
        return out.toByteArray();
    }

    @Override
    public void close() { }
}
