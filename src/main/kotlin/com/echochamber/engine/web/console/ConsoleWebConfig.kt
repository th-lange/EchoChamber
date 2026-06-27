package com.echochamber.engine.web.console

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/** Registers the forced-password-change interceptor on the console paths. */
@Configuration
class ConsoleWebConfig(
    private val forcedPasswordChangeInterceptor: ForcedPasswordChangeInterceptor,
) : WebMvcConfigurer {

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(forcedPasswordChangeInterceptor).addPathPatterns("/admin/**")
    }
}
