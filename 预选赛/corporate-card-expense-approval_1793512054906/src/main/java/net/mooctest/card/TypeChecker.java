package net.mooctest.card;

/** Identifies the card networks supported by the original card validators. */
public final class TypeChecker {
    private TypeChecker() {
    }

    public static CreditCardType checkType(String number) {
        if (new VisaElectronValidator(number).validate()) return CreditCardType.VISA_ELECTRON;
        if (new VisaValidator(number).validate()) return CreditCardType.VISA;
        if (new AmericanExpressValidator(number).validate()) return CreditCardType.AMERICAN_EXPRESS;
        if (new MasterCardValidator(number).validate()) return CreditCardType.MASTERCARD;
        if (new DiscoverValidator(number).validate()) return CreditCardType.DISCOVER;
        if (new MaestroUKValidator(number).validate()) return CreditCardType.MAESTRO_UK;
        if (new MaestroValidator(number).validate()) return CreditCardType.MAESTRO;
        if (new JCBValidator(number).validate()) return CreditCardType.JCB;
        if (new ChinaTUnionValidator(number).validate()) return CreditCardType.CHINA_TUNION;
        if (new ChinaUnionPayValidator(number).validate()) return CreditCardType.CHINA_UNIONPAY;
        if (new DinersClubInternationalValidator(number).validate()) return CreditCardType.DINERS_CLUB_INTERNATIONAL;
        if (new DinersClubValidator(number).validate()) return CreditCardType.DINERS_CLUB;
        if (new RuPayValidator(number).validate()) return CreditCardType.RUPAY;
        if (new InterPaymentValidator(number).validate()) return CreditCardType.INTERPAYMENT;
        if (new InstaPaymenttValidator(number).validate()) return CreditCardType.INSTAPAYMENT;
        if (new DankortValidator(number).validate()) return CreditCardType.DANKORT;
        if (new MIRValidator(number).validate()) return CreditCardType.MIR;
        if (new NPS_PridnestrovieValidator(number).validate()) return CreditCardType.NPS_PRIDNESTROVIE;
        if (new TroyValidator(number).validate()) return CreditCardType.TROY;
        if (new UATPValidator(number).validate()) return CreditCardType.UATP;
        if (new VerveValidator(number).validate()) return CreditCardType.VERVE;
        if (new LankaPayValidator(number).validate()) return CreditCardType.LANKAPAY;
        return CreditCardType.OTHER;
    }
}

