import model.Account;
import model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class QuintTesting {
    private static List<Account> databaseAccount = new ArrayList<>();
    private static List<Transaction> databaseTransaction = new ArrayList<>();

    private static String DEBIT = "D";

    private static String KREDIT = "K";

    public static void main(String[] args) {
        // IDE awal pake 2 property saja account sama table transaction -> akun untuk show informasi umum
        // sudah ada informasi umum di refer ke table transaksi
        // 1. Create Account
        Account EdwardAccount = createAccount("Edward");
        // 1a. Save
        saveAccount(EdwardAccount);

        // 2 Transkasi

        // 2a. Nabung
        Transaction depositTransaction = createNewTransaction(EdwardAccount.getAccountId(),DEBIT,100000L);
        saveTransaction(depositTransaction);


        // 2b. Edward belanja sebesar 20 ribu
        Transaction expenseTransaction = createNewTransaction(EdwardAccount.getAccountId(), KREDIT, 20000L);
        saveTransaction(expenseTransaction);


        // 2c. Total keuangan Edward
        Long saldo = sumTransactionByAccountId(EdwardAccount.getAccountId());


        // Print out
        System.out.println("Account atas nama : "
                + EdwardAccount.getUserName()
                + " memiliki saldo sebesar : "  + saldo
        );

    }

    private static String generateAccountId(){
        return UUID.randomUUID().toString();
    }

    private static Account createAccount(String name){
        String accountId = generateAccountId();
        return new Account(accountId,name);
    }

    private static boolean saveAccount(Account account){
        return databaseAccount.add(account);
    }

    private static List<Account> findAllAccountByAccountId(String accountId){
        return databaseAccount.stream().filter(account -> account.getAccountId().equals(accountId)).toList();
    }

    private static Transaction createNewTransaction(String accountId, String type, Long nominal){
        String transactionId = generateTransactionId();
        return new Transaction(transactionId,accountId,type,nominal);
    }

    private static String generateTransactionId(){
        return UUID.randomUUID().toString();
    }

    private static boolean saveTransaction(Transaction transaction){
        return databaseTransaction.add(transaction);
    }

    private static List<Transaction> findAllTransactionByAccountId(String accountId){
        return databaseTransaction.stream().filter(transaction -> transaction.getAccountId().equals(accountId)).toList();
    }

    private static List<Transaction> filterTransactionByType(List<Transaction> transactions,String type){
        return transactions.stream().filter(transaction -> transaction.getTransactionType().equals(type)).toList();
    }

    private static Long sumTransactionByAccountId(String accountId){
        List<Transaction> transactionsEdwardAccount =  findAllTransactionByAccountId(accountId);
        List<Transaction> debitTransactions = filterTransactionByType(transactionsEdwardAccount,DEBIT);
        List<Transaction> kreditTransactions = filterTransactionByType(transactionsEdwardAccount,KREDIT);

        Long totalDebit = calculateTransactionByTransactions(debitTransactions);
        Long totalKredit = calculateTransactionByTransactions(kreditTransactions);

        return totalDebit - totalKredit;

    }

    private static Long calculateTransactionByTransactions (List<Transaction> transactions){
        Long total = 0L;
        for (Transaction transaction : transactions){
            total += transaction.getNominal();
        }
        return total;
    }






}
