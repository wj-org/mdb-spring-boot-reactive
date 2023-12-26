## About
The purpose of this project is to demonstrate the utilization of the transactional capability of 
reactive Java Spring Boot. 

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


