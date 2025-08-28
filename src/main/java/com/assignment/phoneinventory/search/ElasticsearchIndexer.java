package com.assignment.phoneinventory.search;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.domain.TelephoneNumber;

@Component
public class ElasticsearchIndexer implements ApplicationRunner {

    private final TelephoneNumberDao dao;
    private final TelephoneNumberSearchRepository repo;

    public ElasticsearchIndexer(TelephoneNumberDao dao, TelephoneNumberSearchRepository repo) {
        this.dao = dao;
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        reindexAll();
    }

    public void index(TelephoneNumber t) {
        repo.save(TelephoneNumberDocument.from(t));
    }

    public void reindexAll() {
        List<TelephoneNumberDocument> docs = dao.findAll().stream()
                .map(TelephoneNumberDocument::from)
                .collect(Collectors.toList());
        repo.saveAll(docs);
    }
}

