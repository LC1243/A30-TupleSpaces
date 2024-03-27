package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

import java.util.List;
import java.util.Scanner;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String BGN_TUPLE = "<";
    private static final String END_TUPLE = ">";
    private static final String PUT = "put";
    private static final String READ = "read";
    private static final String TAKE = "take";
    private static final String SLEEP = "sleep";
    private static final String SET_DELAY = "setdelay";
    private static final String EXIT = "exit";
    private static final String GET_TUPLE_SPACES_STATE = "getTupleSpacesState";

    private final ClientService clientService;

    public CommandProcessor(ClientService clientService) {
        this.clientService = clientService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
             switch (split[0]) {
                case PUT:
                    this.put(split);
                    break;

                case READ:
                    this.read(split);
                    break;

                case TAKE:
                    this.take(split);
                    break;

                case GET_TUPLE_SPACES_STATE:
                    this.getTupleSpacesState(split);
                    break;

                case SLEEP:
                    this.sleep(split);
                    break;

                case SET_DELAY:
                    this.setdelay(split);
                    break;

                case EXIT:
                    exit = true;
                    clientService.closeChannels();
                    break;

                default:
                    this.printUsage();
                    break;
             }
        }
        scanner.close();
    }

    private void put(String[] split){

        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // Get and put the tuple
        String tuple = split[1];
        clientService.sendPutRequest(tuple);

    }

    private void read(String[] split){
        // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // Get and read the tuple
        String tuple = split[1];
        clientService.sendReadRequest(tuple);
    }


    private void take(String[] split){
         // check if input is valid
        if (!this.inputIsValid(split)) {
            this.printUsage();
            return;
        }
        
        // get and take the tuple
        String tuple = split[1];
        clientService.sendTakeRequest(tuple);
    }

    private void getTupleSpacesState(String[] split){

        if (split.length != 2){
            this.printUsage();
            return;
        }

        int qualifier = this.indexOfServerQualifier(split[1]);

        // Invalid qualifier
        if (qualifier == -1) {
            try {
                throw new NumberFormatException("Qualifier must be A, B or C");
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                // Propagate the exception to the client or handle it appropriately
                return;
            }
        }
        // Receives the tuples list
        List<String> tuples = clientService.sendGetTupleSpacesStateRequest(split[1]);

        // output stores the list
        String output = "[";

        // Avoids calculating the length of the list every iteration of the loop
        int len_tuples = tuples.size();
        
        // Gets all the tuples
        for (int i = 0; i < len_tuples; i++) {
            output += tuples.get(i);

            if (i < len_tuples - 1) {
                output += ", ";
            }
        }
        // Closes the list
        output += "]\n";

        // Prints the list, in the correct format
        System.out.println(output);

    }

    private void sleep(String[] split) {
      if (split.length != 2){
        this.printUsage();
        return;
      }

      // Number of seconds to sleep
      Integer time;

      // checks if input String can be parsed as an Integer
      try {
         time = Integer.parseInt(split[1]);
      } catch (NumberFormatException e) {
        this.printUsage();
        return;
      }

      // Sleeping
      try {
        Thread.sleep(time*1000);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    private void setdelay(String[] split) {
      if (split.length != 3){
        this.printUsage();
        return;
      }

      // Gets the qualifier
      int qualifier = indexOfServerQualifier(split[1]);
      if (qualifier == -1)
          System.err.println("Invalid server qualifier");
      Integer time;

      // checks if input String can be parsed as an Integer
      try {
        time = Integer.parseInt(split[2]);
      } catch (NumberFormatException e) {
        this.printUsage();
        return;
      }

      // register delay <time> for when calling server <qualifier>
      this.clientService.setDelay(qualifier, time);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- put <element[,more_elements]>\n" +
                "- read <element[,more_elements]>\n" +
                "- take <element[,more_elements]>\n" +
                "- getTupleSpacesState <server>\n" +
                "- sleep <integer>\n" +
                "- setdelay <server> <integer>\n" +
                "- exit\n");
    }

    /* Turns the string qualifier into an integer, -1 if invalid */
    private int indexOfServerQualifier(String qualifier) {
        switch (qualifier) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            default:
                return -1;
        }
    }



    private boolean inputIsValid(String[] input){
        if (input.length < 2 
            ||
            !input[1].substring(0,1).equals(BGN_TUPLE) 
            || 
            !input[1].endsWith(END_TUPLE)
            || 
            input.length > 2
            ) {
            this.printUsage();
            return false;
        }
        else {
            return true;
        }
    }
}
