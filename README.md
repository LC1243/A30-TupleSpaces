# TupleSpaces

Distributed Systems Project 2024

**Group A30**

**Difficulty level: I am Death incarnate!**


### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace __GXX__ with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for 
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members


| Number | Name            | User                              | Email                                         |
|--------|-----------------|-----------------------------------|-----------------------------------------------|
| 103479 | Afonso Matos    | <https://github.com/afonsomatos3> | <mailto:afonso.d.matos@tecnico.ulisboa.pt>    |
| 103860 | Henrique Caroço | <https://github.com/HenriqueProj> | <mailto:henrique.caroco@tecnico.ulisboa.pt>   |
| 103883 | Luís Calado     | <https://github.com/LC1243>       | <mailto:luis.maria.calado@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3). 
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation

To compile and install all modules:

```s
mvn clean install
```

## Built With

* [Maven](https://maven.apache.org/) - Build and dependency management tool;
* [gRPC](https://grpc.io/) - RPC framework.

## Run the project

Name Server (will run without arguments)
```s
python3 server.py
```
Server R1 (without arguments, will run on localhost, port 2001, with qualifier A)
```s
mvn compile exec:java
```
Another way to run Server R1 (if you wish to run with another arguments, the qualifier should still be A)
```s
mvn exec:java -Dexec.args="2001 A"
```

Client (will run without arguments)
```s
mvn compile exec:java
```

