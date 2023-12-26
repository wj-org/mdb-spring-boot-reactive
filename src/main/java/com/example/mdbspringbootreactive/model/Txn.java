package com.example.mdbspringbootreactive.model;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("transactions")
public class Txn {
    public static class Entry {
        public Entry(String accountNum, double amount) {
            this.accountNum = accountNum;
            this.amount = amount;
        }

        public String getAccountNum() {
            return accountNum;
        }

        public void setAccountNum(String accountNum) {
            this.accountNum = accountNum;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        private String accountNum;
        private double amount;
    }
    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    @Id
    private String id;
    private List<Entry> entries;

    private Status status;

    private LocalDateTime transactionDate;

    private ErrorReason errorReason;

    public Txn(List<Entry> entries, Status status, ErrorReason errorReason, LocalDateTime transactionDate) {
        this.entries = entries;
        this.status = status;
        this.errorReason = errorReason;
        this.transactionDate = transactionDate;
    }

    public Txn(){
        this.entries = new ArrayList<>();
        this.status = Status.PENDING;
        this.transactionDate = LocalDateTime.now();
    }

    public void addEntry(Entry entry){
        entries.add(entry);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public ErrorReason getErrorReason() {
        return errorReason;
    }

    public void setErrorReason(ErrorReason errorReason) {
        this.errorReason = errorReason;
    }
}
