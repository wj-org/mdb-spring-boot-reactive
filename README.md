## About
This application runs on reactive Java Spring Boot application with MongoDB Reactive Streams Driver. The project demostrates how you can perform ACID transactions in a very simplifed banking application.

## How it should work
1. A bank account can be created with a unique accountNum, and it always starts with a balance of $0.
2. Accounts and their balances are saved in the "accounts" collection.
3. A debit operation adds to the balance of the account.
4. A credit operation deducts from the balance of the account.
5. A transfer operation deducts from the balance of one account and adds to another.
6. A successful transaction flow is as follows:
   1. Debit/Credit/Transfer operations are first saved in the "transactions" collection with status "PENDING"
   2. The balances of each account is updated accordingly
   3. The transaction status is then updated to "SUCCESS"
7. If there is insufficient balance for deduction in an account, the transaction is rolled back and the status of the transaction is updated to "FAILED"
8. If the account number cannot be found, the transaction is rolled back and the status of the transaction is updated to "FAILED"

## Initialization and Setup
1. Ensure you have access to a MongoDB cluster
2. Run `mongosh "<MongoDB connection string>" --file setup.js` to set up schema validation. This creates a constraint such that the "balance" should never be less than 0.
3. Create application.properties file in resources and add the following lines 
```
spring.data.mongodb.uri=<MongoDB connection string>
spring.data.mongodb.database=txn-demo
 ```
4. Run `mvn clean compile` to compile
5. Run `mvn spring-boot:run` to run the application

## API Usage

### Create account
POST /account \
Request Body:
```
{
  accountNum: <String>,
  balance: <Number>
}
```

### Get account
GET /account/{accountNum}

### Debit to account
POST /account/{accountNum}/debit \
Request Body:
```
{
  amount: <Number>
}
```

### Credit from account
POST /account/{accountNum}/credit \
Request Body:
```
{
  amount: <Number>
}
```

### Transfer to another account
POST /account/{accountNum}/transfer \
Request Body:
```
{
  to: <String>
  amount: <Number>
}
```


