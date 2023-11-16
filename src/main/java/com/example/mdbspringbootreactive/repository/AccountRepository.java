package com.example.mdbspringbootreactive.repository;

import com.example.mdbspringbootreactive.model.Account;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

public interface AccountRepository extends MongoRepository<Account,String> {
    @Query("{accountNum:'?0'}")
    Account findByAccountNum(String accountNum);

    @Update("{'$inc':{'balance': ?1}}")
    int findAndIncrementBalanceByAccountNum(String accountNum, double increment);

}
