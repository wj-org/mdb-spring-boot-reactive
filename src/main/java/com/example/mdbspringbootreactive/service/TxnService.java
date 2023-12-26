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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TxnService {
    @Autowired
    TxnRepository txnRepository;

    @Autowired
    AccountRepository accountRepository;


    public Mono<Txn> savePendingTransaction(Txn txn){
        return txnRepository.save(txn);
    }

    @Transactional
    public Mono<Void> handleTxn(Txn txn){
        return executeTransactionSeq(txn)
                        .doOnError(DataIntegrityViolationException.class, e->{
                            txn.setStatus(Txn.Status.FAILED);
                            txn.setErrorReason(ErrorReason.INSUFFICIENT_BALANCE);
                            txnRepository.save(txn).subscribe();
                        })
                        .doOnError(AccountNotFoundException.class, e->{
                            txn.setStatus(Txn.Status.FAILED);
                            txn.setErrorReason(ErrorReason.ACCOUNT_NOT_FOUND);
                            txnRepository.save(txn).subscribe();
                        })
                        .then(txnRepository.findAndUpdateStatusById(txn.getId(), Txn.Status.SUCCESS));
    }

    public Flux<Long> executeTransactionSeq(Txn txn){
        //update balance for every entry, concatMap maintains the sequence
        Flux<Long> updatedCounts = Flux.fromIterable(txn.getEntries()).concatMap(
                        entry-> accountRepository.findAndIncrementBalanceByAccountNum(entry.getAccountNum(), entry.getAmount()));

        return updatedCounts.handle((updatedCount, sink)->{
                    //1 account should be updated for every entry, otherwise it means account is missing
                    if(updatedCount<1){
                        throw new AccountNotFoundException();
                    }else{
                        sink.next(updatedCount);
                    }
                });
    }

}
