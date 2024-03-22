package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import pt.ulisboa.tecnico.tuplespaces.server.domain.TakeRegistry;

public class ServerState {

  private List<String> tuples;

  // the server qualifier
  private String qualifier;

  private boolean debugMode;

  //next request to process
  int nextSeqNumber;

  // TODO: IMPLEMENT LOCKS INSTEAD OF SYNCHRONIZED
  // Pattern and PriorityQueue of Objects (Object -> seqNumber + Condition + Lock)
  private Map<String, PriorityQueue<TakeRegistry>> takeRequests;

  private Map<String, ArrayList<Condition>> readRequests;

  private Lock putLock;

  private Condition notMyTurn;

  private Lock readLock;

  //TODO: Each tuple should have their own lock ??

  public ServerState(boolean debugMode, String qualifier) {
    this.tuples = new ArrayList<String>();
    this.qualifier = qualifier;
    this.debugMode = debugMode;

    this.nextSeqNumber = 1;

    this.takeRequests = new HashMap<>();

    this.putLock = new ReentrantLock();
    this.notMyTurn = putLock.newCondition();

    this.readRequests = new HashMap<>();
    this.readLock = new ReentrantLock();

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
    //TODO: Locks possiveis:
    //TODO: ReentrantLock, ReentrantReadWriteLock.<Read/Write>Lock
    putLock.lock();
    try {

      while (seqNumber != nextSeqNumber) {
        notMyTurn.await();
      }
      tuples.add(tuple);
      wakeReadRequests(tuple);

      if(!wakeTakeRequest(tuple))
        nextSeqNumber++;
      //TODO: if takes or reads requests waiting -> signal them
    } catch(InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      putLock.unlock();
    }
    //notifyAll();
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
    TakeRegistry takeRegistry = new TakeRegistry(seqNumber);

    //first take of this pattern, initialize Priority Queue
    if(!takeRequests.containsKey(pattern))
      takeRequests.put(pattern, new PriorityQueue<>(new TakeRegistryComparator()));

    //add to the respective pattern priority queue
    takeRequests.get(pattern).offer(takeRegistry);

    System.out.println("PRIORITY QUEUE: " + takeRequests.get(pattern));

    takeRegistry.getLock().lock();

    try {
      while (seqNumber != nextSeqNumber) {
        takeRegistry.getConditionVariable().await();
      }

      String tuple = getMatchingTuple(pattern);

      //while the tuple isn't present in the TupleSpace
      while (tuple == null) {
        if (debugMode) {
          System.err.println("DEBUG: TAKE is waiting for tuple of pattern " + pattern + "\n");
        }

        //still increment the sequence number, so a put request can satisfy this take
        System.out.println("TAKE IS WAITING");
        nextSeqNumber++;
        takeRegistry.getConditionVariable().await();
        tuple = getMatchingTuple(pattern);
      }

      //FIXME: Instead of adding this tuple, pass it directly from put to take
      // remove tuple
      tuples.remove(tuple);
      takeRequests.get(pattern).remove(takeRegistry);
      System.out.println("TAKE is removing the tuple: " + tuple);
      nextSeqNumber++;
      return tuple;

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      takeRegistry.getLock().unlock();
    }
   
  }

  public synchronized List<String> getTupleSpacesState() {
    return new ArrayList<>(this.tuples);
  }


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
    int min_seqNumber = 1000000000;
    List<String> matchingPatterns = getAllMatchingTuplesInTakeRequest(pattern);

    if(matchingPatterns!= null) {
      System.out.println("GOT MATCHING TUPLES");
      // Get the priority queue from the match of pattern and the Map key
      for(String i : matchingPatterns) {
        PriorityQueue<TakeRegistry> queue = takeRequests.get(i);

      /*
       * An empty queue could bring fatal errors. Considering that if we do : take <a> -> put <a> -> put <a> again,
       * the map will have the key "a" but the priority queue will be empty,
       * since the prior put already satisfied the previous take request
      */
      System.out.println("QUEUE PATTERN: " + i + " QUEUE: " + queue);
      if(!queue.isEmpty()){
        TakeRegistry firstElement = queue.peek();
        // Signal to the conditional variable that the take register has been awake
        System.out.println("QUEUE NOT EMPTY, SEQUENCE NUMBER: " + firstElement.getSeqNumber());
        printQueues();
        System.out.println("SeqNum:" + firstElement.getSeqNumber() + " < " + min_seqNumber);
        if(firstElement.getSeqNumber() < min_seqNumber) {
          System.out.println("INSIDE IF");
          min_seqNumber = firstElement.getSeqNumber();
          condition = firstElement.getConditionVariable();
          lock = firstElement.getLock();
        }

        }
      }
      if(min_seqNumber != (1000000000) && condition != null) {
        System.out.println("SIGNALING TAKE WAITING");
        lock.lock();
        try {
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
        // Gets the waiting Conditions
        ArrayList<Condition> conditions = request.getValue();

        // Signal all Conditions
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
  public void printQueues() {
    for(Map.Entry<String,PriorityQueue<TakeRegistry>> request : takeRequests.entrySet())
      System.out.println("PATTERN: " + request.getKey() + " QUEUE:" + request.getValue());
  }
}