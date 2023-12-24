package com.example.mdbspringbootreactive.entity;

public class AccountNotFoundException extends RuntimeException{
    public AccountNotFoundException(){
        super("Account Not Found");
    }
}
