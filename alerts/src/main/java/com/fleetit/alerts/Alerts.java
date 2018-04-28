package com.fleetit.alerts;


import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;

import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;

public class Alerts {
    public static void main(String[] args) {
        Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fleetit-alerts");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_CLUSTER"));

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, JsonNode> updates = builder.stream("updates", Consumed.with(Serdes.String(), jsonSerde));
        KStream<String, JsonNode> speedAlerts = updates.filter((k, v) -> v.get("speed").asDouble() > 5.0);
        speedAlerts.to("alerts-speed", Produced.with(Serdes.String(), jsonSerde));

        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, props);
        CountDownLatch latch = new CountDownLatch(1);

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}