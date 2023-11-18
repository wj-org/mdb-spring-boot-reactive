package com.example.mdbspringbootreactive.controller;

import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.repository.TxnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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
    public Mono<Account> createAccount(@RequestBody Account account){
        return accountRepository.save(account);
    }

    @GetMapping("/account/{accountNum}")
    public Mono<Account> getAccount(@PathVariable String accountNum){
        return accountRepository.findByAccountNum(accountNum);
    }

    @PostMapping("/account/{accountNum}/debit")
    public Mono<Message> debitAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        Mono<Long> updatedCount = accountRepository.findAndIncrementBalanceByAccountNum(accountNum,amount);
        return updatedCount.map(count -> {
            if(count<1) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account Not Found");
            }
            return new Message("success");
        });
    }

    @PostMapping("/account/{accountNum}/credit")
    public Mono<Message> creditAccount(@PathVariable String accountNum, @RequestBody Map<String,Object> requestBody){
        double amount = ((Number)requestBody.get("amount")).doubleValue();
        Mono<Long> updatedCount = accountRepository.findAndIncrementBalanceByAccountNum(accountNum, -amount);
        return updatedCount
                .map(count ->{
                    if(count<1){
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Account Not Found");
                    }
                    return new Message("success");
                })
                .doOnError(DataIntegrityViolationException.class, e->{

                    List<Txn.Entry> entries = new ArrayList<>();
                    entries.add(new Txn.Entry(accountNum,amount));
                    Txn txn = new Txn(entries,Txn.Status.FAILED,Txn.ErrorReason.INSUFFICIENT_BALANCE,LocalDateTime.now());

                    //start async process save transaction;
                    Mono.defer(() -> txnRepository.save(txn))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .onErrorMap(DataIntegrityViolationException.class, e -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Balance"));
    }

    @Transactional
    @PostMapping("/account/{from}/transfer")
    public Mono<Message> transfer(@PathVariable String from, @RequestBody TransferRequest transferRequest){

        String to = transferRequest.to;
        double amount = ((Number)transferRequest.amount).doubleValue();

        Txn txn = new Txn();
        txn.addEntry(new Txn.Entry(from,-amount));
        txn.addEntry(new Txn.Entry(to,amount));

        return txnRepository.save(txn).then(
                performTransaction(txn)
                        .doOnComplete(()-> {
                            Mono.defer(() -> txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.SUCCESS))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                        })
                        .then(Mono.just(new Message("success")))
                        .doOnError(DataIntegrityViolationException.class, e->{
                            txn.setStatus(Txn.Status.FAILED);
                            txn.setErrorReason(Txn.ErrorReason.INSUFFICIENT_BALANCE);
                            //start async process save transaction;
                            Mono.defer(() -> txnRepository.save(txn))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .subscribe();
                        })
                        .onErrorMap(DataIntegrityViolationException.class, e -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient Balance"))
        );
    }

    Flux<Long> performTransaction(Txn txn){
        return Flux.fromIterable(txn.getEntries()).flatMap(entry-> accountRepository.findAndIncrementBalanceByAccountNum(entry.getAccountNum(), entry.getAmount()));
    }

    @Autowired
    ReactiveMongoOperations reactiveOps;

    public static class TransferRequest{
        private String to;
        private double amount;

        public TransferRequest(String to, double amount) {
            this.to = to;
            this.amount = amount;
        }
    }

    public static class Message{
        private String message;
        public Message(String message){
            this.message = message;
        }
        public String getMessage(){
            return this.message;
        }

    }
}
