package jp.co.kawakyo.nextengineapi;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@ImportResource("classpath:META-INF/spring-configuration.xml")
public class NextEngineApiApplication extends SpringBootServletInitializer {

	/**
     * Servletコンテナ起動時の設定クラス認識。
     *
     * <PRE>
     * Servletコンテナで起動したときにどのクラスが設定クラスなのか認識させます。
     * </PRE>
     *
     * @param  SpringApplicationBuilder
     * @return SpringApplicationBuilder
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(NextEngineApiApplication.class);
    }

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		SpringApplication.run(NextEngineApiApplication.class, args);
	}

}
