package com.assignment.phoneinventory.es;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

@Component
public class TelephoneIndexListener {

    private final ElasticsearchOperations operations;

    public TelephoneIndexListener(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Async
    @EventListener
    public void onTelephoneChanged(TelephoneNumberChangedEvent event) {
        operations.save(TelephoneNumberDocument.from(event.getNumber()));
    }
}
