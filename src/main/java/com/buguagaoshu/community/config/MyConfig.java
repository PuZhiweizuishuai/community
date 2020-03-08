package com.buguagaoshu.community.config;


import com.buguagaoshu.community.component.MyLocaleResolver;
import com.buguagaoshu.community.util.JwtUtil;
import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.LocaleResolver;

/**
 * @author Pu Zhiwei {@literal puzhiweipuzhiwei@foxmail.com}
 * create          2019-08-13 16:18
 * 自定义配置
 */
@Configuration
public class MyConfig {
    private final MinIOConfigProperties mcp;

    @Autowired
    public MyConfig(MinIOConfigProperties mcp) {
        this.mcp = mcp;
    }

    /**
     * 将 MyLocaleResolver 添加到容器
     */
    @Bean
    public LocaleResolver localeResolver() {
        return new MyLocaleResolver();
    }


    @Bean
    public JwtUtil jwtUtil() {
        return new JwtUtil();
    }

    @Bean
    public TaskScheduler taskScheduler() {
        return new ThreadPoolTaskScheduler();
    }

    /**
     * MinIO 连接客户端
     * */
    @Bean
    public MinioClient minioClient() throws InvalidPortException, InvalidEndpointException {
        return new MinioClient(mcp.getServer(), mcp.getAccessKey(), mcp.getSecretKey());
    }
}
