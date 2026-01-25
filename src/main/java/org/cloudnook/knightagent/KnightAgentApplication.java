package org.cloudnook.knightagent;

import org.cloudnook.knightagent.core.agent.factory.AgentFactory;
import org.cloudnook.knightagent.core.agent.factory.DefaultAgentFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class KnightAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnightAgentApplication.class, args);
    }

    @Bean
    public AgentFactory agentFactory() {
        return new DefaultAgentFactory();
    }

}
