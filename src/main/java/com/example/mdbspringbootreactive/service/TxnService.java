package com.example.mdbspringbootreactive.service;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.enumeration.TxnStatus;
import com.example.mdbspringbootreactive.exception.AccountNotFoundException;
import com.example.mdbspringbootreactive.exception.TransactionException;
import com.example.mdbspringbootreactive.model.Txn;
import com.example.mdbspringbootreactive.repository.AccountRepository;
import com.example.mdbspringbootreactive.template.TxnTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TxnService {
    private TxnTemplate txnTemplate;

    private AccountRepository accountRepository;

    TxnService(TxnTemplate txnTemplate, AccountRepository accountRepository){
        this.txnTemplate = txnTemplate;
        this.accountRepository = accountRepository;
    }


    public Mono<Txn> saveTransaction(Txn txn){
        return txnTemplate.save(txn);
    }

//------------------------------------------------------
//Using @Transactional annotation to manage transactions
//------------------------------------------------------

//    @Transactional
//    public Mono<Txn> executeTxn(Txn txn){
//        return updateBalances(txn)
//                .onErrorResume(DataIntegrityViolationException.class, e ->{
//                    txn.setStatus(TxnStatus.FAILED);
//                    txn.setErrorReason(ErrorReason.INSUFFICIENT_BALANCE);
//                    return Mono.error(new TransactionException(txn));
//                })
//                .onErrorResume(AccountNotFoundException.class, e ->{
//                    txn.setStatus(TxnStatus.FAILED);
//                    txn.setErrorReason(ErrorReason.ACCOUNT_NOT_FOUND);
//                    return Mono.error(new TransactionException(txn));
//                })
//                .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS));
//    }

//---------------------------------------------------
//Using Transactional Operator to manage transactions
//---------------------------------------------------
    @Autowired
    TransactionalOperator transactionalOperator;

    public Mono<Txn> executeTxn(Txn txn){
        return updateBalances(txn)
                .onErrorResume(DataIntegrityViolationException.class, e ->{
                    txn.setStatus(TxnStatus.FAILED);
                    txn.setErrorReason(ErrorReason.INSUFFICIENT_BALANCE);
                    return Mono.error(new TransactionException(txn));
                })
                .onErrorResume(AccountNotFoundException.class, e ->{
                    txn.setStatus(TxnStatus.FAILED);
                    txn.setErrorReason(ErrorReason.ACCOUNT_NOT_FOUND);
                    return Mono.error(new TransactionException(txn));
                })
                .then(txnTemplate.findAndUpdateStatusById(txn.getId(), TxnStatus.SUCCESS))
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
