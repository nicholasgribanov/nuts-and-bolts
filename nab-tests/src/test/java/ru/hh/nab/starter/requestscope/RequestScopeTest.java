package ru.hh.nab.starter.requestscope;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.ws.rs.core.Response;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import ru.hh.nab.starter.NabApplication;
import ru.hh.nab.testbase.ResourceHelper;
import ru.hh.nab.testbase.extensions.HHJetty;
import ru.hh.nab.testbase.extensions.HHJettyExtension;
import ru.hh.nab.testbase.extensions.OverrideNabApplication;

@ExtendWith({
    HHJettyExtension.class,
})
@SpringJUnitWebConfig({
    RequestConfig.class
})
public class RequestScopeTest {

  @HHJetty(port = 9004, overrideApplication = SpringCtxForJersey.class)
  ResourceHelper resourceHelper;

  @Inject
  private Provider<RequestDetails> requestProvider;

  @Test
  public void requestScopeTest() {

    final String name = requestProvider.get().getField();
    Response response = resourceHelper.target("/hello")
        .queryParam("name", name)
        .request()
        .get();
    assertEquals(OK.getStatusCode(), response.getStatus());
    assertEquals(String.format("Hello, %s!", name), response.readEntity(String.class));
  }

  @Configuration
  @Import(TestResource.class)
  public static class SpringCtxForJersey implements OverrideNabApplication {
    @Override
    public NabApplication getNabApplication() {
      return NabApplication
          .builder()
          .configureJersey(SpringCtxForJersey.class)
          .bindToRoot()
          .build();
    }
  }
}
