package com.assignment.phoneinventory.search;

import java.util.List;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelephoneNumberSearchRepository extends ElasticsearchRepository<TelephoneNumberDocument, Long> {

    List<TelephoneNumberDocument> findByNumberContaining(String number);
}

