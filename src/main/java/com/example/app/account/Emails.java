package com.example.app.account;

import java.util.Locale;

final class Emails {

    private Emails() {}

    /** Les adresses sont comparées sans tenir compte de la casse ni des espaces autour. */
    static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
