package com.assignment.phoneinventory.service;

import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.Operator;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.dto.PageResponse;
import com.assignment.phoneinventory.es.TelephoneNumberDocument;

@Service
public class TelephoneSearchService {

    private final ElasticsearchOperations operations;
    private final TelephoneNumberDao jdbcDao;

    public TelephoneSearchService(ElasticsearchOperations operations, TelephoneNumberDao jdbcDao) {
        this.operations = operations;
        this.jdbcDao = jdbcDao;
    }

    public PageResponse<TelephoneNumber> search(String cc, String ac, String prefix, String contains,
                                                TelephoneNumber.Status status, int page, int size,
                                                boolean fuzzy, boolean wildcard) {
        try {
            BoolQueryBuilder bool = QueryBuilders.boolQuery();
            if (cc != null && !cc.isEmpty()) bool.filter(QueryBuilders.termQuery("countryCode", cc));
            if (ac != null && !ac.isEmpty()) bool.filter(QueryBuilders.termQuery("areaCode", ac));
            if (prefix != null && !prefix.isEmpty()) bool.filter(QueryBuilders.termQuery("prefix", prefix));
            if (status != null) bool.filter(QueryBuilders.termQuery("status", status.name()));

            if (contains != null && !contains.isEmpty()) {
                if (contains.matches("\\d+")) {
                    bool.must(QueryBuilders.prefixQuery("numberDigits", contains));
                } else if (fuzzy) {
                    bool.must(QueryBuilders.matchQuery("number", contains).fuzziness("AUTO"));
                } else if (wildcard) {
                    bool.must(QueryBuilders.wildcardQuery("number", "*" + contains + "*"));
                } else {
                    bool.must(QueryBuilders.matchQuery("number", contains).operator(Operator.AND));
                }
            }

            NativeSearchQueryBuilder builder = new NativeSearchQueryBuilder()
                    .withQuery(bool)
                    .withPageable(PageRequest.of(page, size));
            SearchHits<TelephoneNumberDocument> hits = operations.search(builder.build(), TelephoneNumberDocument.class);
            List<TelephoneNumber> content = hits.getSearchHits().stream()
                    .map(SearchHit::getContent)
                    .map(TelephoneNumberDocument::toDomain)
                    .collect(Collectors.toList());
            long total = hits.getTotalHits();
            return new PageResponse<>(content, page, size, total);
        } catch (Exception ex) {
            List<TelephoneNumber> rows = jdbcDao.search(cc, ac, prefix, contains, status == null ? null : status.name(), page, size);
            long count = jdbcDao.count(cc, ac, prefix, contains, status == null ? null : status.name());
            return new PageResponse<>(rows, page, size, count);
        }
    }
}
