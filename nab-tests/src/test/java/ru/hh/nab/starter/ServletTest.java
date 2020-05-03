package ru.hh.nab.starter;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.GenericServlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.core.Response;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import ru.hh.nab.testbase.NabTestConfig;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.HHJetty;
import ru.hh.nab.testbase.extensions.HHJettyExtension;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@ExtendWith({
    HHJettyExtension.class,
})
@SpringJUnitWebConfig({
    NabTestConfig.class
})
public class ServletTest {

  @HHJetty(port = 9001, overrideApplication = Cfg.class)
  ResourceHelper resourceHelper;

  @Test
  public void testServlet() {
    Response response = resourceHelper.executeGet("/springservlet");
    assertEquals("testValue", response.readEntity(String.class));
  }

  static final class TestServlet extends GenericServlet {

    private final String textValue;

    TestServlet(String textValue) {
      this.textValue = textValue;
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws IOException {
      try (PrintWriter writer = res.getWriter()) {
        writer.write(textValue);
      }
    }
  }

  @Configuration
  public static class Cfg implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication.builder().addServlet(ctx -> ctx.getBean(TestServlet.class), Cfg.class).bindTo("/springservlet").build();
    }

    @Bean
    TestServlet testServlet(Properties serviceProperties) {
      return new TestServlet(serviceProperties.getProperty("customTestProperty"));
    }
  }
}
