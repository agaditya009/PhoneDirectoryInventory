package com.assignment.phoneinventory.batch;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.batch.item.ItemWriter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.assignment.phoneinventory.search.TelephoneDocument;

/**
 * ItemWriter that indexes telephone numbers into Elasticsearch.
 */
public class ElasticsearchItemWriter implements ItemWriter<PhoneCsv> {

    private final ElasticsearchOperations operations;

    public ElasticsearchItemWriter(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public void write(List<? extends PhoneCsv> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        List<TelephoneDocument> docs = items.stream().map(item -> {
            TelephoneDocument doc = new TelephoneDocument();
            doc.setNumber(item.getNumber());
            doc.setCountryCode(item.getCountryCode());
            doc.setAreaCode(item.getAreaCode());
            doc.setStatus("AVAILABLE");
            doc.setAllocatedUserId(item.getAllocatedUserId());
            doc.setReservedUntil(item.getReservedUntil());
            return doc;
        }).collect(Collectors.toList());
        operations.save(docs, operations.getIndexCoordinatesFor(TelephoneDocument.class));
    }
}
