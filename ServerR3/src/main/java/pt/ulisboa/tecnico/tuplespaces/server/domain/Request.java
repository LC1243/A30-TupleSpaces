package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Comparator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Request {
    private int seqNumber;

    private Lock lock;

    private Condition condition;

    Request(int sequenceNumber) {
        this.seqNumber = sequenceNumber;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
    }

    class RequestComparator implements Comparator<Request> {

        // Sort in ascending order of sequence Number
        @Override
        public int compare(Request a, Request b) {
            return a.getSeqNumber() - b.getSeqNumber();
        }
    }

    public int getSeqNumber(){
        return this.seqNumber;
    }

    public void setSeqNumber(int newSeqNumber){
      this.seqNumber = newSeqNumber;
    }

    public Condition getConditionVariable(){
        return this.condition;
    }

    public void setCondition(Condition newCondition){
        this.condition = newCondition;
    }

    public Lock getLock() {
        return this.lock;
    }

    public void setLock(Lock lock) {
        this.lock = lock;
    }


}
