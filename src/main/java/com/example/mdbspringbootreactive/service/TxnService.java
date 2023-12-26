package com.example.mdbspringbootreactive.service;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.exception.AccountNotFoundException;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.repository.TxnRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TxnService {
    @Autowired
    TxnRepository txnRepository;

    @Autowired
    AccountRepository accountRepository;


    public Mono<Txn> saveTransaction(Txn txn){
        return txnRepository.save(txn);
    }

//------------------------------------------------------
//Using @Transactional annotation to manage transactions
//------------------------------------------------------
//
//    @Transactional
//    public Mono<Void> executeTxn(Txn txn){
//        return updateBalances(txn)
//                .doOnError(DataIntegrityViolationException.class, e->{
//                    txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.FAILED,ErrorReason.INSUFFICIENT_BALANCE).subscribe();
//                })
//                .doOnError(AccountNotFoundException.class, e->{
//                    txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.FAILED,ErrorReason.ACCOUNT_NOT_FOUND).subscribe();
//                })
//                .then(txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.SUCCESS));
//    }

//---------------------------------------------------
//Using Transactional Operator to manage transactions
//---------------------------------------------------
    @Autowired
    TransactionalOperator transactionalOperator;

    public Mono<Void> executeTxn(Txn txn){
        return updateBalances(txn)
                .doOnError(DataIntegrityViolationException.class, e->{
                    txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.FAILED,ErrorReason.INSUFFICIENT_BALANCE).subscribe();
                })
                .doOnError(AccountNotFoundException.class, e->{
                    txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.FAILED,ErrorReason.ACCOUNT_NOT_FOUND).subscribe();
                })
                .then(txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.SUCCESS))
                .as(transactionalOperator::transactional);
    }

    public Flux<Long> updateBalances(Txn txn){
        //read entries to update balances, concatMap maintains the sequence
        Flux<Long> updatedCounts = Flux.fromIterable(txn.getEntries()).concatMap(
                        entry-> accountRepository.findAndIncrementBalanceByAccountNum(entry.getAccountNum(), entry.getAmount()));
        return updatedCounts.map(updatedCount->{
            if(updatedCount<1){
                throw new AccountNotFoundException();
            }else{
                return updatedCount;
            }
        });
    }

}
