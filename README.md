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
The clients are in _Client_.
The definition of messages and services is in _Contract_. The naming server
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

### Setting up

First you need to set up the virtual environment, in the home directory, A30-TupleSpaces
1. Create a virtual environment 
```s
python3 -m venv .venv
```
2. Activate the virtual environment
```s
source .venv/bin/activate
```

3. Install grpcio package
```s
python -m pip install grpcio
```
4. Install grpcio-tools package
```s
python -m pip install grpcio-tools
```
5. If you wish to deactivate the virtual environment after running the project
```s
deactivate
```

## Running

### Contract
<br> You should run these commands in the Contract directory

```s
mvn install
```
```s
mvn exec:exec
```

**Name Server** (will run without arguments)
<br> You should run this command in the NameServer directory

```s
python3 server.py
```
### Servers
**Server R2** (without arguments, will run on localhost, port 2001, with qualifier A)
<br> You should run these commands in ServerR2 directory
<br> You should run 3 different servers with different ports and qualifiers (A, B or C)
<br> <br> For example:
- Server 1 (localhost, port 2001, qualifier A)
```s
mvn compile exec:java
```

- Server 2 (localhost, port 2002, qualifier B)
```s
mvn exec:java -Dexec.args="2002 B"
```

- Server 3 (localhost, port 2003, qualifier C)
```s
mvn exec:java -Dexec.args="2003 C"
```

### Client 
(without arguments, will have 1 as his clientId)
<br> You should run these commands in the Client directory
```s
mvn compile exec:java
```

<br>If you wish to run it with another Id, you can run it the following way. Do not give ```0``` as an argument for the clientId.
```s
mvn exec:java -Dexec.args="7"
```
### Debug Mode

You can run the Client, ServerR2 and the NameServer with ```-debug``` flag, to see which requests they are handling while running the project.
