package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The payer of a checkout session. Every field is optional. The widget does not ask
 * the payer for a field that you send. A value that is too long fails with 400
 * {@code VALIDATION_ERROR}, code {@code MAX_LENGTH}.
 */
@JsonPropertyOrder({"firstName", "lastName", "email", "phone", "address", "city", "postalCode", "state"})
public final class Customer {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String address;
    private final String city;
    private final String postalCode;
    private final String state;

    private Customer(Builder b) {
        this.firstName = b.firstName;
        this.lastName = b.lastName;
        this.email = b.email;
        this.phone = b.phone;
        this.address = b.address;
        this.city = b.city;
        this.postalCode = b.postalCode;
        this.state = b.state;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A builder with every field of this object, to change one and build again. */
    public Builder toBuilder() {
        Builder b = new Builder();
        b.firstName = firstName;
        b.lastName = lastName;
        b.email = email;
        b.phone = phone;
        b.address = address;
        b.city = city;
        b.postalCode = postalCode;
        b.state = state;
        return b;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getState() {
        return state;
    }

    /** Builds a {@link Customer}. */
    public static final class Builder {

        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String postalCode;
        private String state;

        private Builder() {
        }

        /** Payer first name. */
        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        /** Payer last name. */
        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        /** Payer email. */
        public Builder email(String email) {
            this.email = email;
            return this;
        }

        /** Payer phone. */
        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        /** Billing street address, one line, at most 200 characters. */
        public Builder address(String address) {
            this.address = address;
            return this;
        }

        /** Billing city, at most 100 characters. */
        public Builder city(String city) {
            this.city = city;
            return this;
        }

        /** Billing postal code, at most 20 characters. */
        public Builder postalCode(String postalCode) {
            this.postalCode = postalCode;
            return this;
        }

        /**
         * Billing state or province code, at most 50 characters. Only used for the United States,
         * Canada and Australia.
         */
        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Customer build() {
            return new Customer(this);
        }
    }
}
