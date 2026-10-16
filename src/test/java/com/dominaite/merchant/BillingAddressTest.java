package com.dominaite.merchant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The billing address of 0.5.2: under customer, after phone, and left off the wire when unset. */
class BillingAddressTest {

    @Test
    void theBillingAddressIsSentUnderCustomer() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, ClientWireTest.SESSION);

            server.client().createCheckoutSession(ClientWireTest.session()
                .customer(Customer.builder()
                    .email("ana@example.com")
                    .phone("+15551234567")
                    .address("1 Market St")
                    .city("San Francisco")
                    .postalCode("94105")
                    .state("CA")
                    .build())
                .build());

            assertEquals("{\"amount\":8440,\"currency\":\"EUR\",\"orderReference\":\"order-1042\",\"customer\":"
                + "{\"email\":\"ana@example.com\",\"phone\":\"+15551234567\",\"address\":\"1 Market St\","
                + "\"city\":\"San Francisco\",\"postalCode\":\"94105\",\"state\":\"CA\"}}", server.last().body);
            assertEquals(server.last().expectedSignature(), server.last().header("X-Signature"));
        }
    }

    @Test
    void anUnsetBillingAddressIsLeftOffTheWire() throws Exception {
        try (MockServer server = new MockServer()) {
            server.respondData(201, ClientWireTest.SESSION);

            server.client().createCheckoutSession(ClientWireTest.session()
                .customer(Customer.builder().email("ana@example.com").build())
                .build());

            assertEquals("{\"amount\":8440,\"currency\":\"EUR\",\"orderReference\":\"order-1042\",\"customer\":"
                + "{\"email\":\"ana@example.com\"}}", server.last().body);
        }
    }

    @Test
    void toBuilderKeepsTheBillingAddress() {
        Customer customer = Customer.builder().address("Andrassy ut 1").city("Budapest").postalCode("1061").build();

        Customer copy = customer.toBuilder().state("BU").build();

        assertEquals("Andrassy ut 1", copy.getAddress());
        assertEquals("Budapest", copy.getCity());
        assertEquals("1061", copy.getPostalCode());
        assertEquals("BU", copy.getState());
    }
}
