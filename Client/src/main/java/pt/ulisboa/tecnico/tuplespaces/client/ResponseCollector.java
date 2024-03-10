package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.ArrayList;

public class ResponseCollector {
    ArrayList<String> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<String>();
    }

    synchronized public void addString(String s) {
        collectedResponses.add(s);
        notifyAll();
    }

    synchronized public String getStrings() {
        String res = new String();
        for (String s : collectedResponses) {
            res = res.concat(s);
        }
        return res;
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n)
            wait();
    }

    synchronized public String getFirstCollectedResponse() {
        return this.collectedResponses.get(0);
    }

    synchronized public ArrayList<String> getCollectedResponses() {
        return collectedResponses;
    }

}
