package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ServerState {

  private List<String> tuples;

  private List<Integer> take_ids;

  private List<Integer> take_locks;

  public ServerState() {
    this.tuples = new ArrayList<String>();
    this.take_ids = new ArrayList<Integer>();
    this.take_locks = new ArrayList<Integer>();
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

  private List<Integer> getAllMatchingTupleIndexes(String pattern) {
    List<Integer> matchingIndexes = new ArrayList<>();

    for (int i = 0; i < this.tuples.size(); i++) {
      String tuple = this.tuples.get(i);
      if (tuple.matches(pattern)) {
        matchingIndexes.add(i);
      }
    }

    return matchingIndexes.isEmpty() ? null : matchingIndexes;
  }

  private List<String> getTuplesByIndexes(List<Integer> indexes) {
    if (indexes == null || indexes.isEmpty()) {
      return null;
    }

    List<String> tuplesByIndexes = new ArrayList<>();

    for (int index : indexes) {
      if (index >= 0 && index < this.tuples.size()) {
        tuplesByIndexes.add(this.tuples.get(index));
      }
    }

    return tuplesByIndexes.isEmpty() ? null : tuplesByIndexes;
  }



  public synchronized List<String> takePhase1(String pattern, Integer clientId) {
    List<Integer> matchingTuples = getAllMatchingTupleIndexes(pattern);

    //while the tuple isn't present in the TupleSpace
    while (matchingTuples == null) {

      try {
        //wait until tuple is available
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      matchingTuples = getAllMatchingTupleIndexes(pattern);
    }

    for (Integer tuple_index : matchingTuples) {
      take_ids.set(tuple_index, clientId);

      if (take_locks.get(tuple_index) == 0)
        take_locks.set(tuple_index, 1);

      //reject request -> return empty list
      else
        return List.of();
    }

    return getTuplesByIndexes(matchingTuples);

  }

  public synchronized int takePhase1Release(Integer clientId) {
    // Invalid ClientId
    if(clientId == 0)
      return 0;
    for (int i = 0; i < this.take_ids.size(); i++) {

      //release Locks
      if(take_ids.get(i) == clientId && take_locks.get(i) == 1) {
        take_locks.set(i, 0);
        take_ids.set(i, 0);
      }

    }

    return 1;

  }

  public <T> void swapAndRemoveLast(List<T> list, int index) {
    if (list == null || index < 0 || index >= list.size() - 1) {
      return;
    }

    // Get the element at the given index
    T elementAtIndex = list.get(index);

    // Swap the element at the given index with the last element
    list.set(index, list.get(list.size() - 1));
    list.set(list.size() - 1, elementAtIndex);

    // Remove the last element
    list.remove(list.size() - 1);
  }

  public synchronized int takePhase2(String tuple, Integer clientId) {
    int tuple_index = tuples.indexOf(tuple);

    if(take_ids.get(tuple_index) == clientId && take_locks.get(tuple_index) == 1) {
      swapAndRemoveLast(take_ids, tuple_index);
      swapAndRemoveLast(take_locks, tuple_index);
      swapAndRemoveLast(tuples, tuple_index);

      //remove remaining locks associated to clientId
      this.takePhase1Release(clientId);

      return 1;
    }

    return 0;
  }

  public synchronized List<String> getTupleSpacesState() {
    return this.tuples;
  }




}
