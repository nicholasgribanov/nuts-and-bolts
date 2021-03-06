package ru.hh.nab.kafka.consumer;

public interface KafkaConsumerFactory {

  <T> KafkaConsumer<T> subscribe(String topicName,
                                 String operationName,
                                 Class<T> messageClass,
                                 ConsumeStrategy<T> messageConsumer);

}
