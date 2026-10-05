package io.github.josemodi97.sageactive4j.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.josemodi97.sageactive4j.graphql.Connection;
import io.github.josemodi97.sageactive4j.graphql.ListOptions;
import io.github.josemodi97.sageactive4j.input.AddressInput;
import io.github.josemodi97.sageactive4j.input.CustomerInput;
import io.github.josemodi97.sageactive4j.input.ProductPriceRequest;
import io.github.josemodi97.sageactive4j.internal.JsonReader;
import io.github.josemodi97.sageactive4j.model.CreatedRecord;
import io.github.josemodi97.sageactive4j.model.Customer;
import io.github.josemodi97.sageactive4j.model.Product;
import io.github.josemodi97.sageactive4j.model.ProductPrice;
import io.github.josemodi97.sageactive4j.model.SalesTariff;
import io.github.josemodi97.sageactive4j.testsupport.DomainTestSupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ThirdPartiesAndProductsTest extends DomainTestSupport {

    @Test
    void customersWithAddressesAndContacts() {
        respond("{\"customers\":{\"nodes\":[{\"id\":\"c1\",\"code\":\"10001\",\"socialName\":\"DUPONT SA\",\"vatNumber\":\"FR33323456789\","
                + "\"addresses\":[{\"city\":\"LYON\",\"isMainAddress\":false},{\"city\":\"PARIS\",\"isMainAddress\":true}],"
                + "\"contacts\":[{\"name\":\"John\",\"surname\":\"Smith\",\"emails\":[{\"emailAddress\":\"john@dupont.fr\",\"usage\":\"INVOICES\"}],"
                + "\"phones\":[{\"number\":\"+33100000000\",\"type\":\"MOBILE\"}]}]}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        Customer customer = sage.thirdParties().customers(ListOptions.defaults()).getNodes().get(0);

        assertEquals("PARIS", customer.getMainAddress().getCity());
        assertEquals("john@dupont.fr", customer.getContacts().get(0).getEmails().get(0).getEmailAddress());
        assertEquals("MOBILE", customer.getContacts().get(0).getPhones().get(0).getType());
        assertContains(query(lastRequest()), "customers(first: $first, after: $after, order: [{ code: ASC }])");
    }

    @Test
    void createCustomerMatchesTheDocumentedFrExample() {
        respond("{\"createCustomer\":{\"id\":\"c-new\",\"code\":\"10042\"}}");

        CreatedRecord created = sage.thirdParties().createCustomer(new CustomerInput()
                .socialName("DUPONT SA").tradeName("Dupont").documentId("323456789").vatNumber("FR33323456789")
                .addAddress(new AddressInput().firstLine("72 rue Joffre").city("PARIS").zipCode("75001")
                        .countryIsoCodeAlpha2("FR")));

        assertEquals("10042", created.getCode());
        Map<String, Object> values = JsonReader.getMap(variables(lastRequest()), "values");
        assertEquals("DUPONT SA", values.get("socialName"));
        @SuppressWarnings("unchecked")
        Map<String, Object> address = (Map<String, Object>) JsonReader.getList(values, "addresses").get(0);
        assertEquals("75001", address.get("zipCode"));
        assertEquals("FR", address.get("countryIsoCodeAlpha2"));
    }

    @Test
    void customerNeedsANameAndAnAddress() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> sage.thirdParties().createCustomer(new CustomerInput().tradeName("x")));
        assertTrue(e.getMessage().contains("socialName") && e.getMessage().contains("addresses"), e.getMessage());
    }

    @Test
    void undocumentedFieldsCanStillBeSent() {
        respond("{\"createCustomer\":{\"id\":\"c\",\"code\":\"1\"}}");
        sage.thirdParties().createCustomer(new CustomerInput().socialName("X")
                .addAddress(new AddressInput().city("Madrid")).set("hasEquivalenceSurcharge", true));
        assertEquals(Boolean.TRUE, JsonReader.getBoolean(variables(lastRequest()), "values", "hasEquivalenceSurcharge"));
    }

    @Test
    void suppliers() {
        respond("{\"suppliers\":{\"nodes\":[{\"id\":\"s1\",\"code\":\"70001\",\"socialName\":\"ACME\"}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        assertEquals("ACME", sage.thirdParties().suppliers(ListOptions.defaults()).getNodes().get(0).getSocialName());
    }

    @Test
    void products() {
        respond("{\"products\":{\"nodes\":[{\"id\":\"p1\",\"code\":\"CONSULT\",\"name\":\"Consulting\",\"category\":\"SERVICE\","
                + "\"salesUnitPrice\":150.00,\"salesVatPercentage\":20}],\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");
        List<Product> products = sage.products().list(ListOptions.defaults()).getNodes();
        assertEquals(new BigDecimal("150.00"), products.get(0).getSalesUnitPrice());
        assertEquals("SERVICE", products.get(0).getCategory());
    }

    @Test
    void priceMatchesTheDocumentedExample() {
        respond("{\"productPriceById\":{\"price\":{\"code\":\"UNKNOWN\",\"name\":\"UNKNOWN\",\"source\":\"UNKNOWN\",\"value\":100},"
                + "\"firstDiscount\":{\"code\":\"UNKNOWN\",\"name\":\"UNKNOWN\",\"source\":\"UNKNOWN\",\"value\":0},"
                + "\"taxGroupId\":\"285afa97-bdf1-45c2-b258-924fd7410082\",\"taxId\":\"808f93b0-b7a2-472d-a0e6-686a4b892778\","
                + "\"equivalenceSurchargePercentage\":0,\"taxPercentage\":20,\"taxTreatmentId\":\"7a58c924-c7b1-4027-9685-e7935e107a85\"}}");

        ProductPrice price = sage.products().price("prod-1", new ProductPriceRequest()
                .customerId("cust-1").documentDate(LocalDate.of(2023, 12, 5)).quantity(new BigDecimal("5")));

        assertEquals(new BigDecimal("100"), price.getUnitPrice());
        assertEquals(new BigDecimal("0"), price.getDiscountPercentage());
        assertEquals(new BigDecimal("20"), price.getTaxPercentage());
        Map<String, Object> vars = variables(lastRequest());
        assertEquals("prod-1", vars.get("id"));
        assertEquals("cust-1", JsonReader.getString(vars, "productPrice", "customerId"));
        assertEquals("2023-12-05", JsonReader.getString(vars, "productPrice", "documentDate"));
        assertContains(query(lastRequest()), "productPriceById(id: $id, productPrice: $productPrice)");
    }

    @Test
    void priceRejectsCustomerAndLeadDocumentTypeTogether() {
        assertThrows(IllegalArgumentException.class, () -> sage.products().price("p",
                new ProductPriceRequest().customerId("c").documentTypeId("d")));
    }

    @Test
    void tariffs() {
        respond("{\"salesTariffs\":{\"nodes\":[{\"id\":\"t-1\",\"code\":\"T01\",\"name\":\"Standard Tariff\","
                + "\"enabled\":true,\"lines\":[{\"id\":\"tl-1\",\"productId\":\"p-1\",\"enabled\":true,\"indicatorValue\":15.50}]}],"
                + "\"pageInfo\":{\"hasNextPage\":false},\"totalCount\":1}}");

        Connection<SalesTariff> tariffs = sage.products().tariffs(ListOptions.first(10));
        assertEquals(1L, tariffs.getTotalCount());
        SalesTariff tariff = tariffs.getNodes().get(0);
        assertEquals("T01", tariff.getCode());
        assertEquals("Standard Tariff", tariff.getName());
        assertTrue(tariff.isEnabled());
        assertEquals(1, tariff.getLines().size());
        assertEquals(new BigDecimal("15.50"), tariff.getLines().get(0).getIndicatorValue());
        assertContains(query(lastRequest()), "salesTariffs(first: $first, after: $after, order: [{ code: ASC }])");
    }
}
