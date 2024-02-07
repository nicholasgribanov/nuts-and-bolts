package ru.hh.nab.starter.filters;

import com.google.common.collect.Lists;
import java.net.URI;
import java.util.Optional;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.uri.UriTemplate;
import org.springframework.util.ClassUtils;
import ru.hh.nab.common.mdc.MDC;
import static ru.hh.nab.common.mdc.MDC.CODE_FUNCTION_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CODE_NAMESPACE_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.CONTROLLER_MDC_KEY;
import static ru.hh.nab.common.mdc.MDC.HTTP_ROUTE_MDC_KEY;

public class ResourceInformationLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
  private static final String SLASH = "/";
  @Inject
  private ResourceInfo resourceInfo;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Class<?> controllerClass = ClassUtils.getUserClass(resourceInfo.getResourceClass());
    String resourceMethodName = resourceInfo.getResourceMethod().getName();
    String controller = controllerClass.getSimpleName() + '#' + resourceMethodName;

    requestContext.setProperty(CONTROLLER_MDC_KEY, controller);
    requestContext.setProperty(CODE_FUNCTION_MDC_KEY, resourceMethodName);
    requestContext.setProperty(CODE_NAMESPACE_MDC_KEY, controllerClass.getCanonicalName());
    getHttpRoute(requestContext.getUriInfo()).ifPresent(route -> requestContext.setProperty(HTTP_ROUTE_MDC_KEY, route));

    MDC.setController(controller);
  }

  private Optional<String> getHttpRoute(UriInfo uriInfo) {
    return ofNullable(uriInfo)
        .filter(ExtendedUriInfo.class::isInstance)
        .map(ExtendedUriInfo.class::cast)
        .map(ExtendedUriInfo::getMatchedTemplates)
        .map(
            templates -> {
              //The base path where the resource is registered or /
              String basePath = of(uriInfo)
                  .map(UriInfo::getBaseUri)
                  .map(URI::getPath)
                  .orElse(SLASH);
              UriBuilder uriBuilder = UriBuilder.fromPath(basePath);
              //Entries in matchedTemplates are ordered in reverse order with the root template last
              Lists.reverse(templates).stream().map(UriTemplate::getTemplate).forEach(uriBuilder::path);
              return uriBuilder.toTemplate();
            }
        );
  }

  @Override
  public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    MDC.clearController();
  }
}
