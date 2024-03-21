package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServerState {

  private List<String> tuples;

  // the server qualifier
  private String qualifier;

  private boolean debugMode;

  public ServerState(boolean debugMode, String qualifier) {
    this.tuples = new ArrayList<String>();
    this.qualifier = qualifier;
    this.debugMode = debugMode;
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
        if(debugMode){
          System.err.println("DEBUG: READ is waiting for tuple of pattern " + pattern + "\n");
        }
        //wait until tuple is available
        wait();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      tuple = getMatchingTuple(pattern);
    }
    return getMatchingTuple(pattern);
  }

  public synchronized String take(String pattern) {
    // get tuple to remove from Tuple Space
    String tuple = this.read(pattern);
    // remove tuple
    tuples.remove(tuple);
    return tuple;

  }

  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(this.tuples);
  }

}
