package net.mooctest.card;

/*
 * Invalid card exception thrown by validator when card
 * does not pass Luhn test.
 */
public class InvalidCardException extends IllegalArgumentException {
    public InvalidCardException(String message) {
        super(message);
    }
}
