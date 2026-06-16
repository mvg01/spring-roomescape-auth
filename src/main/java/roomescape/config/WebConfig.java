package roomescape.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import roomescape.auth.interceptor.LoginCheckInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login",
                        "/members",
                        "/",
                        "/reservation",
                        "/time",
                        "/theme",
                        "/popular",
                        "/my-reservations",
                        "/css/**",
                        "/js/**",
                        "/times/**",
                        "/themes/**",
                        "/reservations/id",
                        "/h2-console/**"
                );
    }
}
