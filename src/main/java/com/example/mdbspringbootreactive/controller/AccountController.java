package com.example.mdbspringbootreactive.controller;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.exception.AccountNotFoundException;
import com.example.mdbspringbootreactive.entity.ResponseMessage;
import com.example.mdbspringbootreactive.entity.TransferRequest;
import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.service.TxnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
public class AccountController {
    @Autowired
    AccountRepository accountRepository;
    @Autowired
    TxnService txnService;

    @PostMapping("/account")
    public Mono<Account> createAccount(@RequestBody Account account){
        return accountRepository.save(account);
    }

    @GetMapping("/account/{accountNum}")
    public Mono<Account> getAccount(@PathVariable String accountNum){
        return accountRepository.findByAccountNum(accountNum)
                .switchIfEmpty(Mono.error(new AccountNotFoundException()));
    }

    @PostMapping("/account/{accountNum}/debit")
    public Mono<ResponseMessage> debitAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        Txn txn = new Txn();
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        txn.addEntry(new Txn.Entry(accountNum,amount));
        return txnService.saveTransaction(txn)
                .flatMap(txnService::executeTxn)
                .then(Mono.just(new ResponseMessage("success")));
    }

    @PostMapping("/account/{accountNum}/credit")
    public Mono<ResponseMessage> creditAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        Txn txn = new Txn();
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        txn.addEntry(new Txn.Entry(accountNum,-amount));
        return txnService.saveTransaction(txn)
                .flatMap(txnService::executeTxn)
                .then(Mono.just(new ResponseMessage("success")));
    }


    @PostMapping("/account/{from}/transfer")
    public Mono<ResponseMessage> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest){
        String to = transferRequest.getTo();
        double amount = ((Number)transferRequest.getAmount()).doubleValue();
        Txn txn = new Txn();
        txn.addEntry(new Txn.Entry(from,-amount));
        txn.addEntry(new Txn.Entry(to,amount));
        //save pending transaction then execute
        return txnService.saveTransaction(txn)
                .flatMap(txnService::executeTxn)
                .then(Mono.just(new ResponseMessage("success")));
    }


    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ResponseMessage> AccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage(ErrorReason.ACCOUNT_NOT_FOUND.name()));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<ResponseMessage> DuplicateAccount(DuplicateKeyException ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage(ErrorReason.DUPLICATE_ACCOUNT.name()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ResponseMessage> InsufficientBalance(DataIntegrityViolationException ex) {
        return ResponseEntity.unprocessableEntity().body(new ResponseMessage(ErrorReason.INSUFFICIENT_BALANCE.name()));
    }

}
