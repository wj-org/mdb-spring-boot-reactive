package com.example.mdbspringbootreactive.repository;

import com.example.mdbspringbootreactive.enumeration.ErrorReason;
import com.example.mdbspringbootreactive.enumeration.TxnStatus;
import com.example.mdbspringbootreactive.model.Txn;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Mono;

public interface TxnRepository extends ReactiveMongoRepository<Txn,String> {

    @Update("{'$set':{'status':'?1'}}")
    Mono<Void> findAndUpdateStatusById(String id, TxnStatus status);

    @Update("{'$set':{'status':'?1', 'errorReason':'?2'}}")
    Mono<Long> findAndUpdateStatusById(String id, TxnStatus status, ErrorReason errorReason);

}
