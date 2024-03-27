package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.grpc.stub.ServerCalls;
import pt.ulisboa.tecnico.tuplespaces.server.domain.Request;

public class ServerState {

  private List<String> tuples;

  // the server qualifier
  private String qualifier;

  private boolean debugMode;

  //next request to process
  int nextSeqNumber;

  // Pattern and PriorityQueue of Objects (Object -> seqNumber + Condition + Lock)
  private Map<String, PriorityQueue<Request>> takeRequests;

  private Map<String, ArrayList<Condition>> readRequests;

  // Take has the lock inside takeRequests
  private Lock readLock;

  //List of requests waiting for their turn to execute (since their sequence Number is different from nextSeqNumber)
  private List<Request> waitingConditions;

  public ServerState(boolean debugMode, String qualifier) {
    this.tuples = new ArrayList<String>();
    this.qualifier = qualifier;
    this.debugMode = debugMode;

    this.nextSeqNumber = 1;

    this.takeRequests = new HashMap<>();

    this.readRequests = new HashMap<>();
    this.readLock = new ReentrantLock();

    this.waitingConditions = new ArrayList<>();

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

  public void put(String tuple, int seqNumber) {
    Request request = new Request(seqNumber);
    request.getLock().lock();
    try {
      // Waits for its turn
      while (seqNumber != nextSeqNumber) {
        if(debugMode) {
          System.out.println("DEBUG: [PUT" + tuple + "] waiting for his turn, SeqNumber:" + seqNumber);
        }

        this.waitingConditions.add(request);
        request.getConditionVariable().await();
      }

      tuples.add(tuple);
      // Signals the read requests waiting for the tuple
      wakeReadRequests(tuple);

      // Wakes the take with the lowest sequence number that's waiting for the tuple
      if(!wakeTakeRequest(tuple)) {
        nextSeqNumber++;
        wakeUpNextRequest();
      }
    } catch(InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      request.getLock().unlock();
    }
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

  public String read(String pattern) {
    readLock.lock();

    Condition condition = readLock.newCondition();

    //first read of this pattern, initialize list of conditions
    if(!readRequests.containsKey(pattern))
      readRequests.put(pattern, new ArrayList<Condition>());

    //add to the respective pattern priority queue
    readRequests.get(pattern).add(condition);
    String tuple = getMatchingTuple(pattern);

    try {
      //while the tuple isn't present in the TupleSpace
      while (tuple == null) {
        if (debugMode) {
          System.err.println("DEBUG: READ is waiting for tuple of pattern " + pattern + "\n");
        }
        //wait until tuple is available
        condition.await();
        tuple = getMatchingTuple(pattern);
      }
      return tuple;
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      readLock.unlock();
    }

  }

  public String take(String pattern, int seqNumber) {
    Request request = new Request(seqNumber);
    boolean added =false;

    /*
    //first take of this pattern, initialize Priority Queue
    if(!takeRequests.containsKey(pattern)) {
      Request.RequestComparator comparator = request.new RequestComparator();
      takeRequests.put(pattern, new PriorityQueue<>(comparator));
    }
    //add to the respective pattern priority queue
    takeRequests.get(pattern).offer(request); */

    request.getLock().lock();

    try {
      // Waits until its turn
      while (seqNumber != nextSeqNumber) {
        if(debugMode) {
          System.out.println("DEBUG: [TAKE" + pattern + "] waiting for his turn, SeqNumber:" + seqNumber);
        }

        this.waitingConditions.add(request);
        request.getConditionVariable().await();
      }
      
      String tuple = getMatchingTuple(pattern);
      if(tuple==null){
        //first take of this pattern, initialize Priority Queue
        if(!takeRequests.containsKey(pattern)) {
          Request.RequestComparator comparator = request.new RequestComparator();
          takeRequests.put(pattern, new PriorityQueue<>(comparator));
        }
        //add to the respective pattern priority queue
        takeRequests.get(pattern).offer(request);
        added = true;
      }

      //while the tuple isn't present in the TupleSpace
      while (tuple == null) {
        if (debugMode) {
          System.err.println("DEBUG: TAKE is waiting for tuple of pattern " + pattern + "\n");
        }

        //still increment the sequence number, so a put request can satisfy this take
        nextSeqNumber++;

        // Still locked after put - wakes the next request and waits
        wakeUpNextRequest();
        request.getConditionVariable().await();
        tuple = getMatchingTuple(pattern);
      }

      // Finished waiting - takes the tuple from server
      tuples.remove(tuple);
      if(added){
        takeRequests.get(pattern).remove(request);
      }


      // Lastly, wake the next request up
      nextSeqNumber++;
      wakeUpNextRequest();
      return tuple;

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      request.getLock().unlock();
    }
   
  }

  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(this.tuples);
  }

  //Returns a list of the patterns of takes requests waiting for a matching tuple, with pattern 'tuple'
  private List<String> getAllMatchingTuplesInTakeRequest(String tuple) {
    List<String> matchingTuples = new ArrayList<>();

    for (String key : takeRequests.keySet()) {
      if (tuple.matches(key)) {
        matchingTuples.add(key);
      }
    }

    return matchingTuples.isEmpty() ? null : matchingTuples;
  }

  /* After a put, wake TakeRequest waiting for the tuple */
  public boolean wakeTakeRequest(String pattern){
    Lock lock = null;
    Condition condition = null;
    // Stores the lowest sequence number of the takes waiting for the pattern
    int min_seqNumber = 1000000000;

    List<String> matchingPatterns = getAllMatchingTuplesInTakeRequest(pattern);

    // Found at least one match
    if(matchingPatterns!= null) {

      if(debugMode) {
        System.out.println("DEBUG: Waking up a take request");
      }

      // Get the priority queue from the match of pattern and the Map key
      for(String i : matchingPatterns) {
        PriorityQueue<Request> queue = takeRequests.get(i);

      /*
       * An empty queue could bring fatal errors. Considering that if we do : take <a> -> put <a> -> put <a> again,
       * the map will have the key "a" but the priority queue will be empty,
       * since the prior put already satisfied the previous take request
      */
      if(!queue.isEmpty()){
        Request firstElement = queue.peek();

        // Signal to the conditional variable that the take register has been awake
        if(firstElement.getSeqNumber() < min_seqNumber) {
          min_seqNumber = firstElement.getSeqNumber();
          condition = firstElement.getConditionVariable();
          lock = firstElement.getLock();
        }

        }
      }
      // Found the waiting take with the smallest sequence number
      if(min_seqNumber != (1000000000) && condition != null) {
        lock.lock();
        try {
          if(debugMode) {
            System.out.println("DEBUG: Waking up take request with pattern: " + pattern + " and seqNumber:" + min_seqNumber);
          }

          // Wakes it up
          condition.signal();
        } finally {
          lock.unlock();
        }
        return true;
      }
    }
    return  false;
  }

  /* After a put, wake all ReadRequests waiting for the tuple */
  public void wakeReadRequests(String tuple){
    // Check if there are ReadRequests waiting for the tuple
    for (Map.Entry<String,ArrayList<Condition>> request : readRequests.entrySet()){

      if (tuple.matches(request.getKey())) {
        if(debugMode) {
          System.out.println("DEBUG: Waking up read requests with tuple: " + tuple);
        }

        // Gets the waiting Conditions
        ArrayList<Condition> conditions = request.getValue();

        // Signal all waiting Read Conditions
        for (Condition waitingCondition: conditions) {
          readLock.lock();
          try {
            waitingCondition.signal();
          } finally {
            readLock.unlock();
          }
        }

        // Lastly, remove the Requests from the waiting list
        readRequests.remove(request);
      }
    }
  }

  public void wakeUpNextRequest() {
    //there isn't a request waiting for his turn
    if (waitingConditions.isEmpty()) {
      return;
    }

    Request nextRequest = waitingConditions.get(0);
    System.out.println("Next Request (Inicio): "+ nextRequest);
    // Get the request with the minimum Sequence Number
    for(Request registry: waitingConditions) {
      if(registry.getSeqNumber() < nextRequest.getSeqNumber())
        nextRequest = registry;
    }
    System.out.println("Next Request (menor number): "+ nextRequest);
    //If it's the next request to execute, wake him up
    if(nextRequest.getSeqNumber() == nextSeqNumber) {
      // Wakes Up the Request
      nextRequest.getLock().lock();
      try {
        if(debugMode) {
          System.out.println("DEBUG: Waking up the next request to execute, Sequence Number:" + nextSeqNumber);
        }

        nextRequest.getConditionVariable().signal();
      } finally {
        nextRequest.getLock().unlock();
      }

      waitingConditions.remove(nextRequest);
    }

  }

}

