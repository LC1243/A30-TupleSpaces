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

    /* This function is used to save a list in the response collector,
    which is delimited by an '|' in the beginning and in the end */
    synchronized public void addAllStrings(List<String> strings) {
        collectedResponses.add("|");
        collectedResponses.addAll(strings);
        collectedResponses.add("|");
        notifyAll();
    }

    synchronized public void waitUntilAllReceived(int n) throws InterruptedException {
        while (collectedResponses.size() < n)
            wait();
    }

    /* Since the lists have a '|' in the beginning and in the end
    the number of lists is the number of '|' dividing by 2 */
    synchronized public long getNumberOfLists() {
        return collectedResponses.stream()
                .filter(element -> element.equals("|"))
                .count() / 2;
    }

    synchronized  public void waitUntilAllListsAreReceived(int n) throws InterruptedException {
        while(this.getNumberOfLists() < n)
            wait();
    }

    /* Since each list is saved in the response collector is delimited by a '|'
     * we loop through the response collector from the end to the beginning
     * to find the '|' that represents the beginning of a list (the 2nd we found),
     *  so we can extract that last list
     */
    synchronized public List<String> getLastListAndRemove() {
        // Find the last occurrence of "|" in the accumulated responses
        int begginingIndex = -1;
        int count = 0;
        for (int i = collectedResponses.size() - 1; i >= 0; i--) {
            if ("|".equals(collectedResponses.get(i))) {
                count++;
                if (count == 2) {
                    begginingIndex = i;
                    break;
                }
            }
        }

        if (begginingIndex == -1) {
            return null; // No "|" found, return null
        }

        // Extract the last list
        List<String> lastList = new ArrayList<>();
        for (int i = begginingIndex + 1; i < collectedResponses.size(); i++) {
            if ("|".equals(collectedResponses.get(i))) {
                break; // Stop when encountering next "|"
            }
            lastList.add(collectedResponses.get(i));
        }

        // Remove the extracted last list and the "|" markers from accumulated responses
        collectedResponses.subList(begginingIndex, collectedResponses.size()).clear();

        return lastList;
    }

    /* Gets the first response received in a read request */
    synchronized public String getFirstCollectedResponse() {
        return this.collectedResponses.get(0);
    }

}
