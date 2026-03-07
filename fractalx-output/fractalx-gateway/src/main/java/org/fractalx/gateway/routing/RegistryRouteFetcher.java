package org.fractalx.gateway.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RegistryRouteFetcher {

    private static final Logger log = LoggerFactory.getLogger(RegistryRouteFetcher.class);

    @Value("${fractalx.registry.url:http://localhost:8761}")
    private String registryUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @SuppressWarnings("unchecked")
    public List<Route> fetchRoutes(RouteLocatorBuilder builder) {
        List<Route> routes = new ArrayList<>();
        try {
            log.debug("Fetching routes from registry at {}", registryUrl);

            List<Map<String, Object>> services =
                    restTemplate.getForObject(registryUrl + "/services", List.class);

            if (services == null || services.isEmpty()) {
                log.debug("No services found in registry");
                return routes;
            }

            RouteLocatorBuilder.Builder routeBuilder = builder.routes();

            for (Map<String, Object> svc : services) {

                if (!"UP".equals(svc.get("status"))) {
                    log.debug("Skipping service {} - status: {}",
                        svc.get("name"), svc.get("status"));
                    continue;
                }

                String name = (String) svc.get("name");
                String host = (String) svc.get("host");
                int port = ((Number) svc.get("port")).intValue();
                String base = toPathBase(name);
                String plural = toPathPlural(name);
                String uri = "http://" + host + ":" + port;

                routeBuilder.route(name + "-live",
                    r -> r.path("/api/" + base + "/**", "/api/" + plural + "/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName(name)
                            .setFallbackUri("forward:/fallback/" + name)))
                        .uri(uri));

                log.info("Live routes added: /api/{}/** and /api/{}/** -> {}", base, plural, uri);
            }

            routeBuilder.build().getRoutes().subscribe(routes::add);

        } catch (ResourceAccessException e) {
            log.warn("Cannot connect to registry at {}: {}", registryUrl, e.getMessage());
        } catch (HttpClientErrorException e) {
            log.warn("Registry returned error: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Could not fetch routes from registry: {}", e.getMessage());
        }
        return routes;
    }

    private String toPathBase(String serviceName) {
        return serviceName.replace("-service", "");
    }

    private String toPathPlural(String serviceName) {
        String path = serviceName.replace("-service", "");
        if (path.endsWith("y")) {
            return path.substring(0, path.length() - 1) + "ies";
        } else if (path.endsWith("s") || path.endsWith("sh") || path.endsWith("ch")) {
            return path + "es";
        } else {
            return path + "s";
        }
    }
}
