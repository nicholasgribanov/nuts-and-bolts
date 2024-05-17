package ru.hh.nab.kafka.consumer;

import java.util.function.BiFunction;
import org.apache.kafka.clients.consumer.Consumer;
import org.slf4j.Logger;
import org.springframework.kafka.support.TopicPartitionOffset;

public interface ConsumerBuilder<T> {

  ConsumerBuilder<T> withClientId(String clientId);

  ConsumerBuilder<T> withOperationName(String operationName);

  ConsumerBuilder<T> withConsumeStrategy(ConsumeStrategy<T> consumeStrategy);

  ConsumerBuilder<T> withLogger(Logger logger);

  ConsumerBuilder<T> withAckProvider(BiFunction<KafkaConsumer<T>, Consumer<?, ?>, Ack<T>> ackProvider);

  ConsumerBuilder<T> withConsumerGroup();

  ConsumerBuilder<T> withAllPartitionsAssigned(TopicPartitionOffset.SeekPosition seekPosition);

  KafkaConsumer<T> start();
}
