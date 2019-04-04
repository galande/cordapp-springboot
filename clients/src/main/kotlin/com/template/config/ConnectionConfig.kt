package com.template.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ConnectionConfig{

    @Bean
    open fun partyAConnection(): NodeRPCConnection{
        return NodeRPCConnection("localhost", "user1", "test", 10006)
    }
}