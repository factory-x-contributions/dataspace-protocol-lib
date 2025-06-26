# Dataspace Protocol Lib
[![Pipeline](https://github.com/factory-x-contributions/dataspace-protocol-lib/actions/workflows/pipeline.yml/badge.svg?logo=GitHub&style=flat-square)](https://github.com/factory-x-contributions/dataspace-protocol-lib/actions/workflows/pipeline.yml)

## Getting started

This project comes in two variants regarding the persistence handling. You can either choose to use the library with 
an attached SQL database or you can choose to use a MongoDB instead. 

### Publishing via MavenLocal

In order to use the library you can, depending on your choice, either build the MongoDB version: 

```
./gradlew dataspace-protocol-lib-mongodb:publishToMavenLocal
```

or the SQL version: 

```
./gradlew dataspace-protocol-lib-sql:publishToMavenLocal
```


The project library should now be available under its classpath and artifact name in your Maven local. It can be imported 
like this: 

```
implementation("org.factoryx.library.connector.embedded:dataspace-protocol-lib-mongodb:<VERSION>>")
```

or, respectively: 
```
implementation("org.factoryx.library.connector.embedded:dataspace-protocol-lib-sql:<VERSION>>")
```


## Requirements on the importing project

The importing project is expected to be a Spring Boot runner application of a current version. It should at least provide 
the following Spring Boot modules: 

```
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
```

Depending on the chose version, you may also need to add
```
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
```

or 
```
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
```


#### Settings for SQL
When using the SQL variant, then you must provide valid settings for the following properties:  
- spring.datasource.driver-class-name
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password

That means, the library is expecting to gain access to an SQL Database like Postgres. But an In-Memory database like 
HSQLDB or H2 will also work (e.g. if you just want to create a simple testing setup). In any case, you also need to provide the 
fitting JDBC drivers for the database system you have chosen. 


#### Settings for MongoDB
For the MongoDB variant, you should provide the following settings: 
- spring.data.mongodb.uri
- spring.data.mongodb.database

### Software-level requirements

#### Implementations you need to provide
In order to couple your importing Spring Boot app with the library, you need to create a class that 
implements the org.factoryx.library.connector.embedded.provider.interfaces.DataAsset interface from the dsp-lib. 

And you need to create an implementation of the org.factoryx.library.connector.embedded.provider.interfaces.DataAssetManagementService, that 
is annotated as "@Service". This implementation is responsible for providing access to the data objects that you want to put 
on offer in the dataspace. You may choose to impose limitations on that data access based on the properties of the requesting partner. 

#### Annotations
Since the classpath of your Spring Boot starter project will likely differ from the library's classpath, you should use 
the following annotations on top of you starter class (the one that has the '@SpringBootApplication' annotation). 

```
@ComponentScan(basePackages = {"org.factoryx.library", "org.demo.testenvironment"})
```

Please replace 'org.demo.testenvironment' with one or more classpath prefixes of your importing project. 

When using the SQL variant, you may also need to add: 

```
@EntityScan(basePackages = {"org.factoryx.library", "org.demo.testenvironment"})
@EnableJpaRepositories(basePackages = {"org.factoryx.library", "org.demo.testenvironment"})
```

With the MongoDB variant, you should add instead: 
```
@EnableMongoRepositories(basePackages = {"org.factoryx.library"})
```

#### Security Setup

You are free to configure the access security of the endpoints in your own, importing project in any way you see fit. 
But you need to make sure that your security measurements do not interfere with the path to the DSP-related endpoints of the library. 
The endpoints of the library are doing their own access control. 

By default, the library is using the "/dsp/**" path. But you can configure this path, see below. 

### Properties settings 

Your importing project should provide the following properties: 

| Property name                              | Meaning                                                                                | Default setting                                                        |
|--------------------------------------------|----------------------------------------------------------------------------------------|------------------------------------------------------------------------|
| org.factoryx.library.hostname              | The dns name of the host, the application is running on.                               | localhost                                                              |
| org.factoryx.library.usetls                | boolean flag that indicates whether TLS is to be used                                  | false                                                                  |
| org.factoryx.library.id                    | The id, that you are using in your dataspace                                           | provider                                                               |
| org.factoryx.library.dspapiprefix          | The prefix that all library-related endpoints are using                                | /dsp                                                                   |
| org.factoryx.library.usebuiltindataccess   | "false" disables the built-in dataacces (not recommended!)                             | true                                                                   | 
| org.factoryx.library.alternativedataaccess | set the host adress and path to an alternative data access endpoint (not recommended!) | localhost                                                              |
| org.factoryx.library.validationservice     | set the type of validation for DSP (currently supported: "mock", "mvd")                | mock                                                                   | 
| org.factoryx.library.mvd.vaultroottoken    | set the token for authorizing access to the MVD provider vault                         | root                                                                   | 
| org.factoryx.library.mvd.vaulturl          | set the url for the MVD provider vault                                                 | http://provider-vault:8200                                             | 
| org.factoryx.library.mvd.vaultsecretalias  | set the secret alias for accessing the provider STS                                    | did%3Aweb%3Aprovider-identityhub%253A7083%3Aprovider-sts-client-secret | 
| org.factoryx.library.mvd.ststokenurl       | set the url of the STS token endpoint                                                  | http://provider-sts-service:8082/api/sts/token                         | 
| org.factoryx.library.mvd.trustedissuer     | set the ID of the MVD trusted issuer                                                   | did:web:dataspace-issuer                                               | 



Please note that the DSP protocol URL will be a result of several settings:

```
http<s>://<org.factoryx.library.hostname>:<server.port>/<org.factoryx.library.dspapiprefix>
```
### Running the tests
This project includes a comprehensive suite of unit tests to ensure the quality and correctness of the library. To run all tests, simply execute:

```
./gradlew test
```






