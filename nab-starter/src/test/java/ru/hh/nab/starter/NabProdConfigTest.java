package ru.hh.nab.starter;

import com.timgroup.statsd.StatsDClient;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.APPEND;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import ru.hh.nab.common.properties.FileSettings;
import static ru.hh.nab.common.properties.PropertiesUtils.SETINGS_DIR_PROPERTY;
import ru.hh.nab.metrics.StatsDSender;
import static ru.hh.nab.starter.NabCommonConfig.SERVICE_NAME_PROPERTY;
import static ru.hh.nab.starter.NabProdConfig.DATACENTER_NAME_PROPERTY;

public class NabProdConfigTest {
  private static final String TEST_SERVICE_NAME = "test-service";
  private static final String TEST_DATACENTER_NAME = "test-dc";
  private static final int TEST_CONSUL_PORT = 13199;

  private Path propertiesFile;
  private Server consulMockServer;

  @BeforeEach
  public void setUp() throws Exception {
    consulMockServer = createConsulMockServer();
    Path tempDir = Files.createTempDirectory("");
    System.setProperty(SETINGS_DIR_PROPERTY, tempDir.toString());
    propertiesFile = createTestPropertiesFile(tempDir);
  }

  @AfterEach
  public void tearDown() throws Exception {
    consulMockServer.stop();
    consulMockServer.destroy();
    System.clearProperty(SETINGS_DIR_PROPERTY);
    Files.deleteIfExists(propertiesFile);
  }

  @Test
  public void testInitContext() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.register(NabProdConfig.class);
    context.refresh();

    assertNotNull(context.getBean(FileSettings.class));

    assertEquals(TEST_SERVICE_NAME, context.getBean(SERVICE_NAME_PROPERTY, String.class));
    assertEquals(TEST_DATACENTER_NAME, context.getBean(DATACENTER_NAME_PROPERTY, String.class));

    assertNotNull(context.getBean(StatsDClient.class));
    assertNotNull(context.getBean(StatsDSender.class));
    assertNotNull(context.getBean("cacheFilter", FilterHolder.class));
    assertNotNull(context.getBean("jettyThreadPool", ThreadPool.class));
    assertNotNull(context.getBean(ScheduledExecutorService.class));
    assertNotNull(context.getBean(AppMetadata.class));
    assertNotNull(context.getBean(ConsulService.class));
  }

  private static Path createTestPropertiesFile(Path dir) throws IOException {
    Path propertiesFile = Files.createFile(Paths.get(dir.toString(), NabProdConfig.PROPERTIES_FILE_NAME));
    List<String> lines = new ArrayList<>();
    lines.add(String.format("%s=%s", SERVICE_NAME_PROPERTY, TEST_SERVICE_NAME));
    lines.add(String.format("%s=%s", DATACENTER_NAME_PROPERTY, TEST_DATACENTER_NAME));
    lines.add(String.format("%s=%s", "jetty.port", "9999"));
    lines.add(String.format("%s=%s", "consul.http.host", "127.0.0.1"));
    lines.add(String.format("%s=%s", "consul.http.port", TEST_CONSUL_PORT));
    lines.add(String.format("%s=%s", "consul.check.host", "127.0.0.1"));
    lines.add(String.format("%s=%s", "consul.check.timeout", "5s"));
    lines.add(String.format("%s=%s", "consul.check.interval", "5s"));
    lines.add(String.format("%s=%s", "consul.tags", ""));
    lines.add(String.format("%s=%s", "consul.enabled", "false"));
    Files.write(propertiesFile, lines, APPEND);
    return propertiesFile;
  }

  private static Server createConsulMockServer() throws Exception {
    InetSocketAddress socketAddress = new InetSocketAddress("localhost", TEST_CONSUL_PORT);
    Server server = new Server(socketAddress);
    server.setHandler(new TestConsulServiceRegisterHandler());
    server.start();
    return server;
  }

  static class TestConsulServiceRegisterHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
      if ("/v1/agent/self".equals(target) || "/v1/agent/service/register".equals(target)) {
        baseRequest.setHandled(true);
        response.setStatus(200);
      }
    }
  }
}
