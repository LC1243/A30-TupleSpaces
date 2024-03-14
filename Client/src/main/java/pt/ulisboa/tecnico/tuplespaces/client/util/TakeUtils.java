package pt.ulisboa.tecnico.tuplespaces.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

//Mostly are auxiliary functions used for the take command
public class TakeUtils {
    public static List<String> getRejectedRequestsQualifiers(List<List<String>> lists) {
        //Since we have the qualifiers of each server at the end position of its list. If the list.size == 1
        // that means we only have the qualifier, none of the tuples matched . So we know the server rejected the request
        return lists.stream()
                .filter(list -> list.size() == 1)
                .map(list -> list.get(0))
                .collect(Collectors.toList());
    }

    public static int getRejectedRequestsQualifierIndex(List<List<String>> lists) {
        for (int i = 0; i < lists.size(); i++) {
            List<String> list = lists.get(i);
            if (list.size() == 1) {
                return i;
            }
        }
        return -1; // Return -1 if no qualifier found
    }

    public static String getSetDifference(List<String> set1, List<String> set2) {
        // Assume set1 has 3 elements and set2 has 2 elements
        // Find the element in set1 that's not in set2
        for (String element : set1) {
            if (!set2.contains(element)) {
                return element;
            }
        }
        // Return null if no difference found
        return null;
    }

    public List<String> findIntersection(List<List<String>> lists) {
        // Return an empty list if there are fewer than two lists
        if (lists == null || lists.size() < 2) {
            return new ArrayList<>();
        }

        // Create a copy of the first list to avoid modifying the original list
        List<String> intersection = new ArrayList<>(lists.get(0));

        // Iterate through the other lists and retain only the elements present in all lists
        for (int i = 1; i < lists.size(); i++) {
            intersection.retainAll(lists.get(i));
        }

        return intersection;
    }

    public String chooseRandomTuple(List<String> intersection) {
        if (intersection.isEmpty()){
            return null;
        }
        Random random = new Random();
        int randomIndex = random.nextInt(intersection.size());

        //Return random tuple
        return intersection.get(randomIndex);
    }
}