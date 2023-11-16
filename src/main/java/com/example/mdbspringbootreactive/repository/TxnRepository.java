package com.example.mdbspringbootreactive.repository;

import com.example.mdbspringbootreactive.model.Account;
import com.example.mdbspringbootreactive.model.Txn;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface TxnRepository extends MongoRepository<Txn,String> {

    @Update("{'$set':{'status':'?1'}}")
    void findAndUpdateStatusById(String id, Txn.Status status);

    @Update("{'$set':{'status':'?1', 'errorReason':'?2', 'errorCode':'?3'}}")
    void findAndUpdateStatusById(String id, Txn.Status status, Txn.ErrorReason errorReason, int errorCode);

}
