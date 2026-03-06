package org.fractalx.gateway.routing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DynamicRouteLocatorConfig {

    private static final Logger log = LoggerFactory.getLogger(DynamicRouteLocatorConfig.class);

    private final RegistryRouteFetcher registryFetcher;
    private final RouteLocatorBuilder builder;

    public DynamicRouteLocatorConfig(RegistryRouteFetcher registryFetcher,
                                     RouteLocatorBuilder builder) {
        this.registryFetcher = registryFetcher;
        this.builder = builder;
    }

    @Bean
    public RouteLocator dynamicRouteLocator() {
        return () -> {
            // Try to fetch live routes from registry first
            try {
                List<Route> liveRoutes = registryFetcher.fetchRoutes(builder);
                if (!liveRoutes.isEmpty()) {
                    log.info("Using {} live routes from fractalx-registry", liveRoutes.size());
                    return Flux.fromIterable(liveRoutes);
                }
                log.warn("No live routes available from registry");
            } catch (Exception e) {
                log.warn("Could not fetch routes from registry: {}", e.getMessage());
            }

            // Fallback to static routes
            log.info("Using static fallback routes");
            return Flux.fromIterable(buildStaticRoutes());
        };
    }

    private List<Route> buildStaticRoutes() {
        List<Route> routes = new ArrayList<>();
        RouteLocatorBuilder.Builder routeBuilder = builder.routes();
        // department-service static route
        routeBuilder.route("department-service-static",
                r -> r.path("/api/department/**", "/api/departments/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("department-service")
                                .setFallbackUri("forward:/fallback/department-service")))
                        .uri("http://localhost:8085"));

        // employee-service static route
        routeBuilder.route("employee-service-static",
                r -> r.path("/api/employee/**", "/api/employees/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("employee-service")
                                .setFallbackUri("forward:/fallback/employee-service")))
                        .uri("http://localhost:8081"));

        // leave-service static route
        routeBuilder.route("leave-service-static",
                r -> r.path("/api/leave/**", "/api/leaves/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("leave-service")
                                .setFallbackUri("forward:/fallback/leave-service")))
                        .uri("http://localhost:8083"));

        // payroll-service static route
        routeBuilder.route("payroll-service-static",
                r -> r.path("/api/payroll/**", "/api/payrolls/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("payroll-service")
                                .setFallbackUri("forward:/fallback/payroll-service")))
                        .uri("http://localhost:8082"));

        // recruitment-service static route
        routeBuilder.route("recruitment-service-static",
                r -> r.path("/api/recruitment/**", "/api/recruitments/**")
                        .filters(f -> f.circuitBreaker(c -> c.setName("recruitment-service")
                                .setFallbackUri("forward:/fallback/recruitment-service")))
                        .uri("http://localhost:8084"));


        routeBuilder.build().getRoutes().subscribe(routes::add);
        log.debug("Built {} static fallback routes", routes.size());
        return routes;
    }
}
