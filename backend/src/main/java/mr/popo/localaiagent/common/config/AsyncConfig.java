package mr.popo.localaiagent.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Java 21 virtual threads pour les tâches @Async et l'exécution SSE.
 * spring.threads.virtual.enabled=true active déjà le Tomcat virtual threads,
 * mais on définit aussi l'executor @Async pour cohérence.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "applicationTaskExecutor")
    public AsyncTaskExecutor applicationTaskExecutor() {
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
