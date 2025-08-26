package com.assignment.phoneinventory.es;

import com.assignment.phoneinventory.domain.TelephoneNumber;

public class TelephoneNumberChangedEvent {
    private final TelephoneNumber number;

    public TelephoneNumberChangedEvent(TelephoneNumber number) {
        this.number = number;
    }

    public TelephoneNumber getNumber() {
        return number;
    }
}
