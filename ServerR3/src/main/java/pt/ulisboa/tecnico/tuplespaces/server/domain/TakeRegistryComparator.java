package pt.ulisboa.tecnico.tuplespaces.server.domain;

import java.util.Comparator;

public class TakeRegistryComparator implements Comparator<TakeRegistry> {

    // Sort in ascending order of sequence Number
    @Override
    public int compare(TakeRegistry a, TakeRegistry b) {
        return a.getSeqNumber() - b.getSeqNumber();
    }
}