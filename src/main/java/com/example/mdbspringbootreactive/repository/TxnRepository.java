package com.example.mdbspringbootreactive.repository;

import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Mono;

public interface TxnRepository extends ReactiveMongoRepository<Txn,String> {

    @Update("{'$set':{'status':'?1'}}")
    Mono<Void> findAndUpdateStatusById(String id, Txn.Status status);

    @Update("{'$set':{'status':'?1', 'errorReason':'?2', 'errorCode':'?3'}}")
    Mono<Long> findAndUpdateStatusById(String id, Txn.Status status, Txn.ErrorReason errorReason, int errorCode);

}
