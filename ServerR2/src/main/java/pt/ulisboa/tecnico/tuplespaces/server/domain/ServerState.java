package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class ServerState {

  private List<String> tuples;

  /* take_ids[i] represents the client who locked tuple of index i (0 if not locked) */
  private List<Integer> take_ids;

  /* take_locks[i] is 1 if tuple of index i is locked (0 otherwise) */
  private List<Integer> take_locks;

  private String qualifier;

  public ServerState(String qualifier) {
    this.tuples = new ArrayList<String>();
    this.take_ids = new ArrayList<Integer>();
    this.take_locks = new ArrayList<Integer>();
    this.qualifier = qualifier;
  }

  public boolean tuppleIsValid(String tuple) {
    if (!tuple.substring(0, 1).equals("<")
            ||
            !tuple.endsWith(">")
            ||
            tuple.contains(" ")
    ) {
      return false;
    } else {
      return true;
    }
  }

  public synchronized void put(String tuple) {
    tuples.add(tuple);
    // Initialize both values to zero (unlocked)
    take_ids.add(0);
    take_locks.add(0);
    notifyAll();
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    // Unsuccessful
    return null;
  }

  public synchronized String read(String pattern) {
    String tuple = getMatchingTuple(pattern);

    //while the tuple isn't present in the TupleSpace
    while (tuple == null) {

      try {
        //wait until tuple is available
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      tuple = getMatchingTuple(pattern);
    }
    return getMatchingTuple(pattern);
  }

  private List<String> getAllMatchingTuples(String pattern) {
    List<String> matchingTuples = new ArrayList<>();

    //Assure that the answer doesn't have repeated
    List<String> tuplesSet = new ArrayList<>();
    for (int i = 0; i < this.tuples.size(); i++) {
      String tuple = this.tuples.get(i);

      if (tuple.matches(pattern) && !matchingTuples.contains(tuple)) {
        matchingTuples.add(tuple);
      }
    }

    return matchingTuples.isEmpty() ? null : matchingTuples;
  }


  public synchronized List<String> takePhase1(String pattern, Integer clientId) {
    List<String> matchingTuples = getAllMatchingTuples(pattern);

    //while the tuple isn't present in the TupleSpace
    while (matchingTuples == null) {

      try {
        //wait until a tuple is available
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      matchingTuples = getAllMatchingTuples(pattern);
    }

    List<String> availableTuples = new ArrayList<>();

    for (String tuple : matchingTuples) {
      int tuple_index = this.tuples.indexOf(tuple);

      // if the tuple is free, lock it
      if (take_locks.get(tuple_index) == 0) {
        System.out.println("Tuple " + this.tuples.get(tuple_index) + " Locked!");
        take_locks.set(tuple_index, 1);
        take_ids.set(tuple_index, clientId);
        availableTuples.add(tuple);
      }
      System.out.println("Tuplo " + tuple + ":    Locked? " + take_locks.get(tuple_index) + ", ID do Cliente: " + take_ids.get(tuple_index));
      // FIXME: Sleep random for second attempt -> Backoff
      // FIXME: Second attempt for all servers??? Or for the ones that returned an empty list
      // FIXME: Adaptar casos para maioria ou minoria
      // Adicionar o qualificador no final da lista
      // Se tamanho da lista == 1 (só tem qualificador), pedido rejeitado -> Se minoria, então release só desse servidor
      // Se a maioria rejeita, release a todos os servidores
      // Cuidado -> Não usar o qualificador na interseção para encontrar um tuplo comum
      // ADICIONAR BACKOFF
    }
    availableTuples.add(qualifier);
    return availableTuples;
  }

  public synchronized int takePhase1Release(Integer clientId) {
    // Invalid ClientId
    if(clientId == 0)
      return 0;
    System.out.println("take_ids_size e clientID (antes do unlock): " + this.take_ids.size() + " " + clientId);
    for (int i = 0; i < this.take_ids.size(); i++) {

      //release Locks
      System.out.println(take_ids.get(i) + " " + take_locks.get(i) + " !");
      if(Objects.equals(take_ids.get(i), clientId) && take_locks.get(i) == 1) {
        System.out.println("Tuple " + this.tuples.get(i) + " Unlocked!");
        take_locks.set(i, 0);
        take_ids.set(i, 0);
        System.out.println("This Server released the tuple!");
      }

    }

    return 1;

  }


  public synchronized int takePhase2(String tuple, Integer clientId) {
    int tuple_index = tuples.indexOf(tuple);

    // Check if clientId locked the tuple
    if(Objects.equals(take_ids.get(tuple_index), clientId) && take_locks.get(tuple_index) == 1) {
      take_ids.remove(tuple_index);
      take_locks.remove(tuple_index);
      tuples.remove(tuple_index);

      //remove remaining locks associated to clientId
      this.takePhase1Release(clientId);

      return 1;
    }

    return 0;
  }

  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(this.tuples);
  }

}
