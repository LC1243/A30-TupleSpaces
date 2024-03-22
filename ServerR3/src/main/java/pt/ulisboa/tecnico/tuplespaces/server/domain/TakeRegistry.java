package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


import pt.ulisboa.tecnico.tuplespaces.server.domain.TakeRegistryComparator;

public class TakeRegistry {
    private int seqNumber;

    private Lock lock;

    private Condition condition;

    TakeRegistry(int sequenceNumber) {
        this.seqNumber = sequenceNumber;
        this.lock = new ReentrantLock();
        this.condition = lock.newCondition();
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
