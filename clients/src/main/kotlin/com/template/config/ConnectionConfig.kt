package com.template.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ConnectionConfig{

    @Bean
    open fun centralBankConnection(): NodeRPCConnection{
        return NodeRPCConnection("localhost", "user1", "test", 10006)
    }

    @Bean
    open fun municipalCorpConnection():NodeRPCConnection{
        return NodeRPCConnection("localhost","user1","test", 10009)
    }

    @Bean
    open fun homesEstate():NodeRPCConnection{
        return NodeRPCConnection("localhost","user1","test", 10012)
    }

    @Bean
    open fun abilDevConnection():NodeRPCConnection{
        return NodeRPCConnection("localhost","user1","test", 10015)
    }

    @Bean
    open fun paranjpeConstructionConnection():NodeRPCConnection{
        return NodeRPCConnection("localhost","user1","test", 10018)
    }

}