# TupleSpaces

Distributed Systems Project 2024

**Group A30**

**Difficulty level: I am Death incarnate!**

The goal of this project was to develop the **TupleSpace** system, a service that implements a distributed _tuple space_ using gRPC and Java (along with a Python NameServer). There are **three different implementations**, each in their respective branches, explained further below.

## What is a TupleSpace?

The service allows one or more users (also called _workers_ in the literature) to place tuples in the shared space, read existing tuples, as well as remove tuples from the space. A tuple is an ordered set of fields _<field_1, field_2, ..., field_n>_.
In this project, a tuple must be instantiated as a _string_, for example, `"<vacancy,sd,shift1>"`.

In the TupleSpace, several identical instances can co-exist.
For example, there may be multiple tuples `"<vacancy,sd,turno1>"`, indicating the existence of several vacancies.

It is possible to search, in the space of tuples, for a given tuple to read or remove.
In the simplest variant, one can search for a concrete tuple. For example, `"<vacancy,sd,shift1>"`.
Alternatively, you can use [Java regular expressions](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html#sum) to allow pairing with multiple values. For example, `"<vacancy,sd,[^,]+>"` pairs with `"<vacancy,sd,turno1>"` as well as `"<vacancy,sd,turno2>"`.

The operations available to the user are the following: _put_, _read_, _take_ and _getTupleSpacesState_.

* The *put* operation adds a tuple to the shared space.

* The *read* operation accepts the tuple description (possibly with regular expression) and returns *a* tuple that matches the description, if one exists. This operation blocks the client until a tuple that satisfies the description exists. The tuple is *not* removed from the tuple space.

* The *take* operation accepts the tuple description (possibly with regular expression) and returns *a* tuple that matches the description. This operation blocks the client until a tuple that satisfies the description exists. The tuple *is* removed from the tuple space.

* The *getTupleSpacesState* operation receives as its only argument the qualifier of the server to be queried and returns all tuples present on that server.

Users access the **TupleSpace** service through a client process, which interacts
with one or more servers that offer the service, through calls to remote procedures.

## Implementations:

* **R1** (on branch first-delivery): The service is provided by a single server (i.e. a simple client-server architecture, without server replication), which accepts requests at a fixed address/port.

* **R2** (on branch second-delivery): The service is replicated in three servers (A, B and C), following an adaptation of the [Xu-Liskov algorithm](http://www.ai.mit.edu/projects/aries/papers/programming/linda.pdf). This algorithm performs the _take_ operation in two steps to ensure consistency (explained in more detail in the R2 branch).

* **R3** (on branch third-delivery): This implementation is based on the **State Machine Replication** (SMR) approach, an alternative to the Xu/Liskov algorithm. This approach focuses on ensuring total order for operations between the three replicas.

Note that both Variant 2 and Variant 3 have advantages and disadvantages, and may present better or worse performance depending on the pattern of use of the tuple space.

You can also find in each branch a `README.md`, explaining how to run the project.

---------

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
