package com.assignment.phoneinventory.service;

import java.util.ArrayList;
import java.util.List;

import com.assignment.phoneinventory.domain.TelephoneNumber;
import com.assignment.phoneinventory.search.TelephoneNumberDocument;
import com.assignment.phoneinventory.search.TelephoneNumberSearchRepository;
import org.springframework.stereotype.Service;

@Service
public class TelephoneSearchService {

    private final TelephoneNumberSearchRepository repository;

    public TelephoneSearchService(TelephoneNumberSearchRepository repository) {
        this.repository = repository;
    }

    public Iterable<TelephoneNumber> search(String countryCode,
                                            String areaCode,
                                            String contains,
                                            TelephoneNumber.Status status) {
        Iterable<TelephoneNumberDocument> docs =
                (contains == null || contains.isEmpty())
                        ? repository.findAll()
                        : repository.findByNumberContaining(contains);

        List<TelephoneNumber> numbers = new ArrayList<>();
        for (TelephoneNumberDocument doc : docs) {
            if (countryCode != null && !countryCode.equals(doc.getCountryCode())) {
                continue;
            }
            if (areaCode != null && !areaCode.equals(doc.getAreaCode())) {
                continue;
            }
            if (status != null && status != doc.getStatus()) {
                continue;
            }
            numbers.add(doc.toDomain());
        }
        return numbers;
    }
}

