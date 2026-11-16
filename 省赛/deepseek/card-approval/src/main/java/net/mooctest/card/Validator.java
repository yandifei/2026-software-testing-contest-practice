package net.mooctest.card;

import java.time.Clock;
import java.util.Objects;

/*
 * Validator used to consume credit card number, expiration
 * date & CVV for validation
 */
public class Validator {
    private final String creditCardNumber;
    private final StringBuilder expirationDate;
    private Integer expirationYear;
    private Integer expirationMonth;
    private final String CVV;
    private final Clock clock;

    /*
     * @param String representation of credit card number
     */
    public Validator(String creditCardNumber) {
        this.creditCardNumber = Objects.requireNonNull(creditCardNumber, "number must not be null");
        this.expirationDate = null;
        this.CVV = null;
        this.clock = Clock.systemDefaultZone();
    }

    /*
     * @param String representation of credit card number,
     * expiration date & CVV
     */
    public Validator(String creditCardNumber, String expirationDate, String CVV) {
        this(creditCardNumber, expirationDate, CVV, Clock.systemDefaultZone());
    }

    /** Accepts MMyy or MM/yy and a clock that tests can fix explicitly. */
    public Validator(String creditCardNumber, String expirationDate, String CVV, Clock clock) {
        this.creditCardNumber = Objects.requireNonNull(creditCardNumber, "number must not be null");
        Objects.requireNonNull(expirationDate, "expirationDate must not be null");
        if (!expirationDate.matches("(0[1-9]|1[0-2])/?[0-9]{2}")) {
            throw new IllegalArgumentException("expirationDate must use MMyy or MM/yy");
        }
        this.expirationDate = DateParser.parseDate(DateChecker.convertDate(expirationDate));
        this.CVV = CVV;
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        setExpirationMonth();
        setExpirationYear();
    }

    /*
     * Set expiration year
     */
    public void setExpirationYear() {
        this.expirationYear = DateParser.parseDate(requireExpirationDate(), 2, 4);
    }

    /*
     * Set expiration month
     */
    public void setExpirationMonth() {
        this.expirationMonth = DateParser.parseDate(requireExpirationDate(), 0, 2);
    }

    /*
     * Validate credit card number using Luhn algorithm
     * @throws InvalidCardException if card does not pass
     * test.
     */
    public boolean validate() throws InvalidCardException {
        if (!creditCardNumber.matches("[0-9]{12,19}")) {
            throw new InvalidCardException("Card number must contain 12 to 19 digits");
        }
        LuhnValidator luhnValidator = new LuhnValidator();
        boolean isValid = luhnValidator.validate(creditCardNumber);

        if (!isValid) {
            throw new InvalidCardException("Card number checksum is invalid");
        }

        return isValid;
    }

    /*
     * @return whether the CVV contains three or four digits
     */
    public boolean checkCVV() {
        return CVV != null && CVV.matches("[0-9]{3,4}");
    }

    /*
     * @return whether the card is valid through the current month
     */
    public boolean checkExpirationDate() {
        return expirationMonth != null && expirationYear != null
                && DateChecker.isValidExpiration(expirationMonth, expirationYear, clock);
    }

    private StringBuilder requireExpirationDate() {
        if (expirationDate == null) {
            throw new IllegalStateException("expirationDate was not supplied");
        }
        return expirationDate;
    }
}
