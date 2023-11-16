package com.example.mdbspringbootreactive.controller;

import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.repository.TxnRepository;
import com.mongodb.MongoWriteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class AccountController {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TxnRepository txnRepository;

    @PostMapping("/account")
    public Account createAccount(@RequestBody Account account){
        System.out.println(account);
        return accountRepository.save(account);
    }

    @GetMapping("/account/{accountNum}")
    public Account getAccount(@PathVariable String accountNum){
        return accountRepository.findByAccountNum(accountNum);
    }

    @PostMapping("/account/{accountNum}/debit")
    public ResponseEntity<String> debitAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        double amount = (double) requestBody.get("amount");
        int updatedCount = accountRepository.findAndIncrementBalanceByAccountNum(accountNum,amount);
        if(updatedCount<1){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("{\"message\": \"success\"}");
    }

    @PostMapping("/account/{accountNum}/credit")
    public ResponseEntity<String> creditAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        double amount = (double) requestBody.get("amount");
        try {
            int updatedCount = accountRepository.findAndIncrementBalanceByAccountNum(accountNum, -amount);
            if (updatedCount < 1) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok("{\"message\": \"success\"}");
        }catch(DataIntegrityViolationException e){
            System.out.println(e.getMessage());

            //log failed transaction
            List<Txn.Entry> entries = new ArrayList<>();
            entries.add(new Txn.Entry(accountNum,amount));
            Txn txn = new Txn(entries,Txn.Status.FAILED,Txn.ErrorReason.INSUFFICIENT_BALANCE,LocalDateTime.now());
            Txn txnSaved = txnRepository.save(txn);

            return ResponseEntity.unprocessableEntity().body("{\"message\": \"Insufficient balance\"}");
        }

    }

    @PostMapping("/account/{from}/transfer")
    public ResponseEntity<String> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest){

        String to = transferRequest.to;
        double amount = transferRequest.amount;

        Txn txn = new Txn();
        txn.addEntry(new Txn.Entry(from,-amount));
        txn.addEntry(new Txn.Entry(to,amount));

        txn = txnRepository.save(txn);

        try{
            performTransaction(txn);
            txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.SUCCESS);
        }catch(DataIntegrityViolationException e){
            System.out.println(e.getMessage());
            txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.FAILED, Txn.ErrorReason.INSUFFICIENT_BALANCE,Txn.ErrorReason.INSUFFICIENT_BALANCE.code);
            return ResponseEntity.unprocessableEntity().body("{\"message\": \"Insufficient balance\"}");
        }


        return ResponseEntity.ok("{\"message\": \"success\"}");
    }

    @Transactional
    void performTransaction(Txn txn){
        for(Txn.Entry entry : txn.getEntries()){
            accountRepository.findAndIncrementBalanceByAccountNum(entry.getAccountNum(),entry.getAmount());
        }
    }

    public static class TransferRequest{
        private String to;
        private double amount;

        public TransferRequest(String to, double amount) {
            this.to = to;
            this.amount = amount;
        }

    }
}
