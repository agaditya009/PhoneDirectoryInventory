package com.assignment.phoneinventory.service;

import java.util.stream.Collectors;

import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.search.TelephoneNumberDocument;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TelephoneSearchService {

    private final ElasticsearchOperations operations;

    public TelephoneSearchService(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    /**
     * Execute a search against Elasticsearch applying the provided filters.
     *
     * @param countryCode optional country code filter
     * @param areaCode optional area/region code filter
     * @param contains optional fragment of the telephone number
     * @param status optional status of the number
     * @return numbers matching all supplied criteria
     */
    public Iterable<TelephoneNumber> search(String countryCode,
                                            String areaCode,
                                            String contains,
                                            TelephoneNumber.Status status) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

        if (StringUtils.hasText(contains)) {
            boolQuery.must(QueryBuilders.wildcardQuery("number", "*" + contains + "*"));
        } else {
            boolQuery.must(QueryBuilders.matchAllQuery());
        }
        if (StringUtils.hasText(countryCode)) {
            boolQuery.filter(QueryBuilders.termQuery("countryCode", countryCode));
        }
        if (StringUtils.hasText(areaCode)) {
            boolQuery.filter(QueryBuilders.termQuery("areaCode", areaCode));
        }
        if (status != null) {
            boolQuery.filter(QueryBuilders.termQuery("status", status.name()));
        }

        NativeSearchQuery query = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .build();

        return operations.search(query, TelephoneNumberDocument.class).stream()
                .map(SearchHit::getContent)
                .map(TelephoneNumberDocument::toDomain)
                .collect(Collectors.toList());
    }
}

