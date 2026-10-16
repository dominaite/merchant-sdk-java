package com.dominaite.merchant;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * The payer of a checkout session. Every field is optional. The widget does not ask
 * the payer for a field that you send.
 */
@JsonPropertyOrder({"firstName", "lastName", "email", "phone"})
public final class Customer {

    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;

    private Customer(Builder b) {
        this.firstName = b.firstName;
        this.lastName = b.lastName;
        this.email = b.email;
        this.phone = b.phone;
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

    /** Builds a {@link Customer}. */
    public static final class Builder {

        private String firstName;
        private String lastName;
        private String email;
        private String phone;

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

        public Customer build() {
            return new Customer(this);
        }
    }
}
