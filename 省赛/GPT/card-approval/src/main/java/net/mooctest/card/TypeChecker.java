package net.mooctest.card;

import java.util.Objects;

/** Card-format identification; a successful match does not imply a valid checksum. */
public final class TypeChecker {
    // Specific prefixes precede broad formats. Shared 54/55 prefixes default to Mastercard.
    private static final CreditCardType[] CHECK_ORDER = {
            CreditCardType.NPS_PRIDNESTROVIE, CreditCardType.LANKAPAY,
            CreditCardType.VERVE, CreditCardType.TROY, CreditCardType.VISA_ELECTRON,
            CreditCardType.MAESTRO_UK, CreditCardType.DANKORT, CreditCardType.VISA,
            CreditCardType.AMERICAN_EXPRESS, CreditCardType.MASTERCARD,
            CreditCardType.DISCOVER, CreditCardType.JCB, CreditCardType.CHINA_TUNION,
            CreditCardType.CHINA_UNIONPAY, CreditCardType.DINERS_CLUB_INTERNATIONAL,
            CreditCardType.RUPAY, CreditCardType.INTERPAYMENT, CreditCardType.INSTAPAYMENT,
            CreditCardType.MIR, CreditCardType.UATP, CreditCardType.MAESTRO
    };

    private TypeChecker() {
    }

    public static CreditCardType checkType(String number) {
        Objects.requireNonNull(number, "number must not be null");
        if (!number.matches("[0-9]{12,19}")) {
            return CreditCardType.OTHER;
        }
        for (CreditCardType type : CHECK_ORDER) {
            if (matchesType(number, type)) {
                return type;
            }
        }
        return CreditCardType.OTHER;
    }

    /** Checks an explicit network, including formats whose prefixes overlap other networks. */
    public static boolean matchesType(String number, CreditCardType type) {
        Objects.requireNonNull(number, "number must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (!number.matches("[0-9]{12,19}")) {
            return false;
        }
        return switch (type) {
            case VISA -> new VisaValidator(number).validate();
            case MASTERCARD -> new MasterCardValidator(number).validate();
            case DISCOVER -> new DiscoverValidator(number).validate();
            case AMERICAN_EXPRESS -> new AmericanExpressValidator(number).validate();
            case VISA_ELECTRON -> new VisaElectronValidator(number).validate();
            case MAESTRO -> new MaestroValidator(number).validate();
            case MAESTRO_UK -> new MaestroUKValidator(number).validate();
            case JCB -> new JCBValidator(number).validate();
            case CHINA_TUNION -> new ChinaTUnionValidator(number).validate();
            case CHINA_UNIONPAY -> new ChinaUnionPayValidator(number).validate();
            case DINERS_CLUB_INTERNATIONAL -> new DinersClubInternationalValidator(number).validate();
            case DINERS_CLUB -> new DinersClubValidator(number).validate();
            case RUPAY -> new RuPayValidator(number).validate();
            case INTERPAYMENT -> new InterPaymentValidator(number).validate();
            case INSTAPAYMENT -> new InstaPaymenttValidator(number).validate();
            case DANKORT -> new DankortValidator(number).validate();
            case MIR -> new MIRValidator(number).validate();
            case NPS_PRIDNESTROVIE -> new NPS_PridnestrovieValidator(number).validate();
            case TROY -> new TroyValidator(number).validate();
            case UATP -> new UATPValidator(number).validate();
            case VERVE -> new VerveValidator(number).validate();
            case LANKAPAY -> new LankaPayValidator(number).validate();
            case OTHER -> false;
        };
    }
}

