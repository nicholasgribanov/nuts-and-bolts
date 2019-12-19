package ru.hh.nab.kafka.util;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.kafka.util.ConfigProvider.COMMON_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_CONSUMER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.DEFAULT_PRODUCER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.TOPIC_CONSUMER_CONFIG_TEMPLATE;
import static ru.hh.nab.kafka.util.ConfigProvider.TOPIC_PRODUCER_CONFIG_TEMPLATE;
import java.util.Map;
import java.util.Properties;

public class ConfigProviderTest {

  private static final String SERVICE_NAME = "testService";
  private static final String KAFKA_CLUSTER_NAME = "kafka";

  @Test
  public void shouldReturnCommonSettings() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(COMMON_CONFIG_TEMPLATE, testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldContainServiceNameSetting() {
    var result = createConfigProvider(createFileSettings(Map.of())).getConsumerConfig("ignored");

    assertEquals(SERVICE_NAME, result.get(ConsumerConfig.CLIENT_ID_CONFIG));
  }

  @Test
  public void shouldReturnConsumerDefaultSetting() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getConsumerConfig("ignored");

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldReturnOverriddenConsumerSettingForSpecificTopic() {
    String testKey = "key";
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String topicName = "topic";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_CONSUMER_CONFIG_TEMPLATE, testKey), defaultValue,
        generateSettingKey(TOPIC_CONSUMER_CONFIG_TEMPLATE, topicName, testKey), overriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(fileSettings);

    var result = configProvider.getConsumerConfig(topicName);
    assertEquals(overriddenValue, result.get(testKey));

    result = configProvider.getConsumerConfig("ignored");
    assertEquals(defaultValue, result.get(testKey));
  }

  @Test
  public void shouldReturnDefaultProducerSetting() {
    String testKey = "key";
    String testValue = "value";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_PRODUCER_CONFIG_TEMPLATE, testKey), testValue
    ));

    var result = createConfigProvider(fileSettings).getProducerConfig("ignored");

    assertEquals(testValue, result.get(testKey));
  }

  @Test
  public void shouldReturnOverriddenProducerSettingForSpecificTopic() {
    String testKey = "key";
    String defaultValue = "value";
    String overriddenValue = "newValue";
    String topicName = "topic";
    FileSettings fileSettings = createFileSettings(Map.of(
        generateSettingKey(DEFAULT_PRODUCER_CONFIG_TEMPLATE, testKey), defaultValue,
        generateSettingKey(TOPIC_PRODUCER_CONFIG_TEMPLATE, topicName, testKey), overriddenValue
    ));

    ConfigProvider configProvider = createConfigProvider(fileSettings);

    var result = configProvider.getProducerConfig(topicName);
    assertEquals(overriddenValue, result.get(testKey));

    result = configProvider.getProducerConfig("ignored");
    assertEquals(defaultValue, result.get(testKey));
  }

  private static String generateSettingKey(String template, String testKey) {
    return String.format(template, KAFKA_CLUSTER_NAME) + "." + testKey;
  }

  private static String generateSettingKey(String template, String topicName, String testKey) {
    return String.format(template, KAFKA_CLUSTER_NAME, topicName) + "." + testKey;
  }

  private static ConfigProvider createConfigProvider(FileSettings fileSettings) {
    return new ConfigProvider(SERVICE_NAME, KAFKA_CLUSTER_NAME, fileSettings);
  }

  private static FileSettings createFileSettings(Map<String, Object> props) {
    Properties properties = new Properties();
    properties.putAll(props);
    return new FileSettings(properties);
  }
}