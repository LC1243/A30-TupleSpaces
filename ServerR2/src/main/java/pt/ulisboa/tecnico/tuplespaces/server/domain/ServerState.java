package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerState {

  private List<String> tuples;

  /* take_ids[i] represents the client who locked tuple of index i (0 if not locked) */
  private List<Integer> take_ids;

  /* take_locks[i] is 1 if tuple of index i is locked (0 otherwise) */
  private List<Integer> take_locks;

  // the server qualifier
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

    //Assure that the answer doesn't have repeated tuples
    // allowing to another clients use the other tuples (possibly for a take operation)
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

      // if the tuple is free, lock it and associate it with the respective ClientId
      if (take_locks.get(tuple_index) == 0) {
        take_locks.set(tuple_index, 1);
        take_ids.set(tuple_index, clientId);
        availableTuples.add(tuple);
      }

    }
    availableTuples.add(qualifier);
    return availableTuples;
  }

  public synchronized int takePhase1Release(Integer clientId) {
    // Invalid ClientId (clientId 0 is reserved for tuples initialization)
    if(clientId == 0)
      return 0;
    for (int i = 0; i < this.take_ids.size(); i++) {

      //release Locks acquired by the client with id clientId
      if(Objects.equals(take_ids.get(i), clientId) && take_locks.get(i) == 1) {
        take_locks.set(i, 0);
        take_ids.set(i, 0);
      }

    }

    return 1;

  }


  public synchronized int takePhase2(String tuple, Integer clientId) {
    // Since we locked a tuple in tuples , we have to perform a loop
    // to find the exact tuple we locked, characterized by its clientId.
    // This way we avoid the problem of trying to remove a tuple that may be the same
    // as the one we should remove, but doesn't have the correct clientId.
    for (int i = 0; i < tuples.size(); i++) {
      if (Objects.equals(take_ids.get(i), clientId) && take_locks.get(i) == 1
              && tuple.equals(this.tuples.get(i))) {
        take_ids.remove(i);
        take_locks.remove(i);
        tuples.remove(i);

        //remove remaining locks associated to clientId
        this.takePhase1Release(clientId);

        return 1;
      }
    }
      return 0;
  }

  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(this.tuples);
  }

}
