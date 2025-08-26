package com.assignment.phoneinventory;

import javax.annotation.PostConstruct;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;

import com.assignment.phoneinventory.es.TelephoneNumberDocument;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String esUrl;

    @Bean
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration config = ClientConfiguration.builder()
                .connectedTo(esUrl)
                .build();
        return RestClients.create(config).rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }

    @Autowired
    private ElasticsearchOperations operations;

    @PostConstruct
    public void createIndex() {
        IndexOperations indexOps = operations.indexOps(TelephoneNumberDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping(indexOps.createMapping());
        }
    }
}
