package com.example.mdbspringbootreactive.controller;

import com.example.mdbspringbootreactive.entity.AccountNotFoundException;
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
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        return accountRepository.findAndIncrementBalanceByAccountNum(accountNum,amount).flatMap(updatedCount ->{
            if(updatedCount<1){
                throw new AccountNotFoundException();
            }
            return Mono.just(new ResponseMessage("success"));
        });
    }

    @PostMapping("/account/{accountNum}/credit")
    public Mono<ResponseMessage> creditAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        return accountRepository.findAndIncrementBalanceByAccountNum(accountNum,-amount).flatMap(updatedCount ->{
            if(updatedCount<1){
                throw new AccountNotFoundException();
            }
            return Mono.just(new ResponseMessage("success"));
        });
    }

//    @PostMapping("/account/{accountNum}/debit")
//    public Mono<Message> debitAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
//        Txn txn = new Txn();
//        double amount = ((Number)requestBody.get("amount")).doubleValue();
//        txn.addEntry(new Txn.Entry(accountNum,amount));
//        return txnService.handleTransaction(txn);
//    }
//
//    @PostMapping("/account/{accountNum}/credit")
//    public Mono<Message> creditAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
//        Txn txn = new Txn();
//        double amount = ((Number)requestBody.get("amount")).doubleValue();
//        txn.addEntry(new Txn.Entry(accountNum,-amount));
//        return txnService.handleTransaction(txn);
//    }

//    @Transactional
//    @PostMapping("/account/{from}/transfer")
//    public Mono<Message> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest){
//        String to = transferRequest.getTo();
//        double amount = ((Number)transferRequest.getAmount()).doubleValue();
//        Txn txn = new Txn();
//        txn.addEntry(new Txn.Entry(from,-amount));
//        txn.addEntry(new Txn.Entry(to,amount));
//        return txnService.handleTransaction(txn);
//    }
@PostMapping("/account/{from}/transfer")
public Mono<ResponseMessage> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest){
    String to = transferRequest.getTo();
    double amount = ((Number)transferRequest.getAmount()).doubleValue();
    Txn txn = new Txn();
    txn.addEntry(new Txn.Entry(from,-amount));
    txn.addEntry(new Txn.Entry(to,amount));
    return txnService.savePendingTransaction(txn)
            .then(txnService.handleTxn(txn))
            .then(Mono.just(new ResponseMessage("success")));
//    return txnService.handleTransaction(txn);
}



    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity AccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity DuplicateAccount(DuplicateKeyException ex) {
        return ResponseEntity.badRequest().body(new ResponseMessage("Duplicate Account"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity InsufficientBalance(DataIntegrityViolationException ex) {
        return ResponseEntity.unprocessableEntity().body(new ResponseMessage("Insufficient Balance"));
    }

}
