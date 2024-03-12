package pt.ulisboa.tecnico.tuplespaces.client;

import java.util.ArrayList;
import java.util.List;

public class ResponseCollector {
    ArrayList<String> collectedResponses;

    public ResponseCollector() {
        collectedResponses = new ArrayList<String>();
    }

    synchronized public void addString(String s) {
        collectedResponses.add(s);
        notifyAll();
    }
    synchronized public void addAllStrings(List<String> strings, boolean first) {
        if (first)
            this.addString("|");

        collectedResponses.addAll(strings);
        this.addString("|");
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

    synchronized public long getNumberOfLists() {
        return collectedResponses.stream()
                .filter(element -> element.equals("|"))
                .count();
    }

    synchronized  public void waitUntilAllListsAreRecieved(int n) throws InterruptedException {
        while(this.getNumberOfLists() < n)
            wait();
    }

    public List<String> getLastListAndRemove() {
        if (collectedResponses.isEmpty()) {
            return null; // No accumulated responses
        }

        // Find the last occurrence of "|" in the accumulated responses
        int lastIndex = -1;
        for (int i = collectedResponses.size() - 1; i >= 0; i--) {
            if (collectedResponses.get(i).equals("|")) {
                lastIndex = i;
                break;
            }
        }

        if (lastIndex == -1) {
            return null; // No "|" found, return null
        }

        // Extract the last list
        List<String> lastList = new ArrayList<>();
        for (int i = lastIndex + 1; i < collectedResponses.size(); i++) {
            lastList.add(collectedResponses.get(i));
        }

        // Remove the extracted last list and the "|" marker from accumulated responses
        collectedResponses.subList(lastIndex, collectedResponses.size()).clear();

        return lastList;
    }

    synchronized public String getFirstCollectedResponse() {
        return this.collectedResponses.get(0);
    }

    synchronized public ArrayList<String> getCollectedResponses() {
        return collectedResponses;
    }

    synchronized public void setCollectedResponses(ArrayList<String> collectedResponses) {
        this.collectedResponses = collectedResponses;
    }

    synchronized public void removeLastElement() {
        if (!collectedResponses.isEmpty()) {
            collectedResponses.remove(collectedResponses.size() - 1);
        }
    }

}
