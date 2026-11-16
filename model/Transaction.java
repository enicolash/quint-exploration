package model;

public class Transaction {


    private String transactionId;
    private String accountId;
    private String transactionType;

    private Long nominal;

    public Transaction(String transactionId, String accountId, String transactionType, Long nominal) {
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.nominal = nominal;
    }

    public Long getNominal() {
        return nominal;
    }

    public void setNominal(Long nominal) {
        this.nominal = nominal;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getTransactionType() {
        return transactionType;
    }
}
