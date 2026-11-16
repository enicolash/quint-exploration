package model;
public class Account {
    public Account(String accountId, String userName) {
        this.accountId = accountId;
        this.userName = userName;
    }

    private String accountId;
    private String userName;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
