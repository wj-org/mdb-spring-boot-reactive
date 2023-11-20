# About
The purpose of this project is to demonstrate the utilization of the transactional capability of 
reactive Java Spring Boot. 

## Initialization and Setup
1. Ensure you have access to a MongoDB cluster
2. Run `mongosh "<MongoDB connection string>" --file setup.js` to setup schema validation. This creates a constraint such that the "balance" should never be less than 0.
3. Create application.properties file in resources and add the following lines 
```
spring.data.mongodb.uri=<MongoDB connection string>
spring.data.mongodb.database=txn-demo
 ```
4. Run `mvn clean compile` to compile
5. Run `mvn spring-boot:run` to run the application

## API (To be added)
