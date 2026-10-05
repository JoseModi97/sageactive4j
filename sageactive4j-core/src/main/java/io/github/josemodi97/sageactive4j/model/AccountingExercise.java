package io.github.josemodi97.sageactive4j.model;

import java.time.LocalDate;
import java.util.Map;

/** A fiscal year. */
public final class AccountingExercise extends SageObject {

    public AccountingExercise(Map<String, Object> json) {
        super(json);
    }

    /** The year, e.g. {@code 2026}. */
    public Integer getExercise() { return integer("exercise"); }
    public String getDescription() { return string("description"); }
    public LocalDate getStartDate() { return date("startDate"); }
    public LocalDate getEndDate() { return date("endDate"); }
    public Integer getNumberPeriods() { return integer("numberPeriods"); }
    /** {@code OPEN}, {@code CLOSED} or {@code UNDEFINED}. */
    public String getStatus() { return string("status"); }

    public boolean isOpen() { return "OPEN".equals(getStatus()); }
}
