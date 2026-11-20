package com.catalogo.peliculas.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métricas propias de trazabilidad.
 *
 * app_build_version{version="0.0.1-SNAPSHOT", commit="<sha>"} = 1
 *
 * Grafana usa esta métrica para mostrar qué versión y qué commit están desplegados,
 * cerrando la trazabilidad commit -> imagen Docker -> pod en Kubernetes.
 */
@Configuration
public class ObservabilidadConfig {

    @Bean
    MeterBinder versionDesplegada(@Value("${info.app.version:desconocida}") String version,
                                  @Value("${info.app.commit:local}") String commit) {
        return registry -> Gauge.builder("app.build.version", () -> 1)
                .description("Versión y commit del microservicio en ejecución (valor fijo 1)")
                .tag("version", version)
                .tag("commit", commit)
                .register(registry);
    }
}
