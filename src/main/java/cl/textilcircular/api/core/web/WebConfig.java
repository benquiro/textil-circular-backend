package cl.textilcircular.api.core.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** Every feature controller lives under the contract's base path; actuator and docs stay at the root. */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    public static final String API_BASE_PATH = "/api/v1";

    @Value("${app.media.dir:media-storage}")
    private Path mediaDir;

    /** Uploaded images are public (the app loads them as plain links) and live outside the API prefix. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**").addResourceLocations(mediaDir.toAbsolutePath().toUri().toString());
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_BASE_PATH, HandlerTypePredicate.forBasePackage("cl.textilcircular.api.feature"));
    }
}
