package com.assignment.phoneinventory.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TelephoneSearchRepository extends ElasticsearchRepository<TelephoneDocument, Long> {
}
