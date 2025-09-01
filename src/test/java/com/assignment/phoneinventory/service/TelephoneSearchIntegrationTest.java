package com.assignment.phoneinventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.search.ElasticsearchIndexer;
import com.assignment.phoneinventory.search.TelephoneNumberDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class TelephoneSearchIntegrationTest {

    @Container
    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.13")
            .withEnv("discovery.type", "single-node");

    @DynamicPropertySource
    static void elasticProps(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.rest.uris", elastic::getHttpHostAddress);
    }

    @Autowired
    private TelephoneNumberDao dao;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    private ElasticsearchIndexer indexer;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setupIndex() {
        IndexOperations indexOps = operations.indexOps(TelephoneNumberDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.create();
    }

    @Test
    void containsSearchReturnsExpectedHits() {
        dao.upsertNumber("5551234567", "1", "2");
        dao.upsertNumber("5559876543", "1", "3");
        indexer.reindexAll();

        ResponseEntity<TelephoneNumber[]> response = restTemplate.getForEntity("/api/phones/elastic?contains=123", TelephoneNumber[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> numbers = Arrays.stream(response.getBody())
                .map(TelephoneNumber::getNumber)
                .collect(Collectors.toList());
        assertThat(numbers).contains("5551234567");
        assertThat(numbers).doesNotContain("5559876543");
    }

    @Test
    void filtersByCountryAreaAndStatusReturnExpectedHits() {
        dao.upsertNumber("1110000000", "1", "200");
        dao.upsertNumber("2220000000", "44", "300");
        dao.upsertNumber("3330000000", "1", "400");
        TelephoneNumber reserved = dao.findByNumber("2220000000").get();
        reserved.setStatus(TelephoneNumber.Status.RESERVED);
        dao.updateWithVersion(reserved.getId(), reserved.getVersion(), reserved);
        indexer.reindexAll();

        ResponseEntity<TelephoneNumber[]> byCountry = restTemplate.getForEntity("/api/phones/elastic?countryCode=44", TelephoneNumber[].class);
        assertThat(byCountry.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> countryNumbers = Arrays.stream(byCountry.getBody()).map(TelephoneNumber::getNumber).collect(Collectors.toList());
        assertThat(countryNumbers).containsExactly("2220000000");

        ResponseEntity<TelephoneNumber[]> byArea = restTemplate.getForEntity("/api/phones/elastic?areaCode=400", TelephoneNumber[].class);
        assertThat(byArea.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> areaNumbers = Arrays.stream(byArea.getBody()).map(TelephoneNumber::getNumber).collect(Collectors.toList());
        assertThat(areaNumbers).containsExactly("3330000000");

        ResponseEntity<TelephoneNumber[]> byStatus = restTemplate.getForEntity("/api/phones/elastic?status=RESERVED", TelephoneNumber[].class);
        assertThat(byStatus.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> statusNumbers = Arrays.stream(byStatus.getBody()).map(TelephoneNumber::getNumber).collect(Collectors.toList());
        assertThat(statusNumbers).containsExactly("2220000000");
    }
}
