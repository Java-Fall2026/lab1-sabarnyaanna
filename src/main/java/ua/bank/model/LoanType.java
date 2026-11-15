package ua.bank.model;

public enum LoanType {
    MORTGAGE(0.07, "Іпотечне кредитування"),
    CONSUMER(0.18, "Споживчий кредит"),
    AUTO(0.12, "Автокредитування"),
    COMMERCIAL(0.15, "Комерційний кредит");

    private final double annualRate;
    private final String description;

    LoanType(double annualRate, String description) {
        this.annualRate = annualRate;
        this.description = description;
    }

    public double getAnnualRate() {
        return annualRate;
    }

    public String getDescription() {
        return description;
    }
}