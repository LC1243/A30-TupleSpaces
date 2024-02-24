package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.ArrayList;
import java.util.List;

public class ServerState {

  private List<String> tuples;

  public ServerState() {
    this.tuples = new ArrayList<String>();
  }
  public boolean tuppleIsValid(String tuple){
    if (!tuple.substring(0,1).equals("<")
            ||
            !tuple.endsWith(">")
            ||
            tuple.contains(" ")
    ) {
      return false;
    }
    else {
      return true;
    }
  }

  public void put(String tuple) {
    tuples.add(tuple);
  }

  private String getMatchingTuple(String pattern) {
    for (String tuple : this.tuples) {
      if (tuple.matches(pattern)) {
        return tuple;
      }
    }
    return null;
  }

  public String read(String pattern) {
    return getMatchingTuple(pattern);
  }

  public String take(String pattern) {
    // TODO
    return null;
  }

  public List<String> getTupleSpacesState() {
    // TODO
    return null;
  }

  public String ListToString() {
    StringBuilder sb = new StringBuilder();
    for (String tuple : tuples) {
      sb.append(tuple).append(" "); // You can adjust the separator as per your requirement
    }
    return sb.toString().trim(); // Trim to remove trailing space

  }
}
