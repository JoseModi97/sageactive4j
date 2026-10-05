package io.github.josemodi97.sageactive4j.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** A commercial payment term definition. */
public final class PaymentTerm extends SageObject {

    public PaymentTerm(Map<String, Object> json) {
        super(json);
    }

    public String getName() { return string("name"); }
    public OffsetDateTime getModificationDate() { return dateTime("modificationDate"); }
    public List<Line> getLines() { return list(Line::new, "lines"); }

    /** One line (installment rule) of a {@link PaymentTerm}. */
    public static final class Line extends SageObject {
        public Line(Map<String, Object> json) {
            super(json);
        }

        public String getType() { return string("type"); }
        public String getPaymentMeanId() { return string("paymentMeanId"); }
        public Integer getDay() { return integer("day"); }
        public String getCondition() { return string("condition"); }
        public Integer getOrder() { return integer("order"); }
        public Integer getPayDays() { return integer("payDays"); }
    }
}
