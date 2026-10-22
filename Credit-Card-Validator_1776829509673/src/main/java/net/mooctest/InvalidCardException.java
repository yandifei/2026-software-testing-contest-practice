package net.mooctest;

/*
 * Invalid card exception thrown by validator when card
 * does not pass Luhn test.
 */
class InvalidCardException extends IllegalArgumentException {
    InvalidCardException(String message) {
        super(message);
    }
}
