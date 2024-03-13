package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
import com.google.protobuf.ProtocolStringList;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.nameServer.contract.NameServer;
import pt.ulisboa.tecnico.nameServer.contract.NameServerServiceGrpc;
import pt.ulisboa.tecnico.tuplespaces.client.ClientObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.*;
import io.grpc.StatusRuntimeException;

import pt.ulisboa.tecnico.tuplespaces.client.ResponseCollector;
import pt.ulisboa.tecnico.tuplespaces.client.util.OrderedDelayer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static io.grpc.Status.INVALID_ARGUMENT;


public class ClientService {

    private ArrayList<String> targets = new ArrayList<String>();

    private ArrayList<String> qualifiers = new ArrayList<String>();

    private boolean debugMode = false;
    OrderedDelayer delayer;

    private int numServers;

    private ManagedChannel[] channels;
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    private int clientId;

    public ClientService(boolean debugMode, int numServers, int clientId) {

        this.numServers = numServers;
        delayer = new OrderedDelayer(numServers);
        this.debugMode = debugMode;
        this.lookupServer();

        this.clientId = clientId;

        channels = new ManagedChannel[numServers];
        stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];

        // Creates a channel and stub for every server
        for (int i = 0; i < numServers; i++) {
            channels[i] = ManagedChannelBuilder.forTarget(targets.get(i)).usePlaintext().build();
            stubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
        }


    }

    /* This method allows the command processor to set the request delay assigned to a given server */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);
    }

    public void closeChannels() {
        if(debugMode)
            System.err.println("DEBUG: Shutting down channels\n");

        for (ManagedChannel ch : channels)
            ch.shutdown();
    }


    // Lookup for Servers that can satisfy the service TupleSpace
    public void lookupServer() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        try {
            NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);
            NameServer.LookupRequest request = NameServer.LookupRequest.newBuilder().setService("TupleSpace").build();

            // Get the ip and port where the server is running
            NameServer.LookupResponse response = stub.lookup(request);

            com.google.protobuf.ProtocolStringList servers = new LazyStringArrayList();
            servers = response.getServerList();

            // Found servers
            if (!servers.isEmpty()) {
                for(int i = 0; i < response.getServerCount() - 1; i += 2) {
                    String target = response.getServer(i);
                    targets.add(target);
                    qualifiers.add(response.getServer(i+1));
                }

            } else {
                System.err.println("There aren't servers available. Please try run again");
                return;
            }
        } catch (StatusRuntimeException e) {
            System.err.println("Error communicating with the NameServer during lookup: " + e.getStatus().getDescription());
        } finally {
            channel.shutdownNow();
        }
    }


    /* TODO: individual methods for each remote operation of the TupleSpaces service */

    /* Example: How to use the delayer before sending requests to each server
     *          Before entering each iteration of this loop, the delayer has already
     *          slept for the delay associated with server indexed by 'id'.
     *          id is in the range 0..(numServers-1).

        for (Integer id : delayer) {
            //stub[id].some_remote_method(some_arguments);
        }

    */


    public void sendPutRequest(String tuple) {

        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Put Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaXuLiskov.PutRequest> requests = new ArrayList<TupleSpacesReplicaXuLiskov.PutRequest>();

        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaXuLiskov.PutRequest request = TupleSpacesReplicaXuLiskov.PutRequest.newBuilder().setNewTuple(tuple).build();
            requests.add(request);
        }

        for(Integer id : delayer) {

            try {
                stubs[id].put(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.PutResponse>(c));

            // Exception caught
            } catch (StatusRuntimeException e) {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }


        }

        //wait all responses
        try {
            c.waitUntilAllReceived(numServers);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("OK\n");

        if (debugMode) {
            System.err.println("DEBUG: PutRequest finished correctly\n");
        }

    }

    public void sendReadRequest(String tuple) {

        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Read Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaXuLiskov.ReadRequest> requests = new ArrayList<TupleSpacesReplicaXuLiskov.ReadRequest>();


        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaXuLiskov.ReadRequest request = TupleSpacesReplicaXuLiskov.ReadRequest.newBuilder().setSearchPattern(tuple).build();
            requests.add(request);

        }


        for(Integer id : delayer) {
            try {
                // Send the request to the server with the id
                stubs[id].read(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.ReadResponse>(c));

            } catch (StatusRuntimeException e) {
                // Handle status runtime exception
                System.out.println("Caught exception with description: " + e.getStatus().getDescription());
            }

        }

        //wait for the first response
        try {
            c.waitUntilAllReceived(1);
            System.out.println("OK");
            System.out.println(c.getFirstCollectedResponse() + "\n");
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (debugMode) {
            System.err.println("DEBUG: Read Request finished correctly\n");
        }
    }

    public void sendTakeRequest(String tuple) {

       // obtain matching tuples from all servers
       List<List<String>> lists = sendTakePhase1Request(tuple);

       boolean containsEmptyList = lists.stream().anyMatch(List::isEmpty);

       /* In case a list returned by a server is empty (request rejected)
        *  release locks and repeat Phase1 */
       if (containsEmptyList) {
           sendTakePhase1ReleaseRequest();
           sendTakeRequest(tuple);
       }

       //find the intersection between matching tuples of all servers
       List<String> intersection = findIntersection(lists);

       //if intersection is null, release acquired locks and repeat the process
       if(intersection.isEmpty()) {
           sendTakePhase1ReleaseRequest();
           sendTakeRequest(tuple);
       }


       String toRemove = chooseRandomTuple(intersection);
       sendTakePhase2Request(toRemove);
    }

    public List<String> findIntersection(List<List<String>> lists) {
        if (lists == null || lists.size() < 2) {
            return new ArrayList<>(); // Return an empty list if there are fewer than two lists
        }

        // Create a copy of the first list to avoid modifying the original list
        List<String> intersection = new ArrayList<>(lists.get(0));

        // Iterate through the other lists and retain only the elements present in all lists
        for (int i = 1; i < lists.size(); i++) {
            intersection.retainAll(lists.get(i));
        }

        return intersection;
    }


    public List<List<String>> sendTakePhase1Request(String pattern) {

        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Take Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaXuLiskov.TakePhase1Request> requests =
                new ArrayList<TupleSpacesReplicaXuLiskov.TakePhase1Request>();

        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaXuLiskov.TakePhase1Request request =
                    TupleSpacesReplicaXuLiskov.TakePhase1Request.newBuilder().setSearchPattern(pattern).setClientId(clientId).build();
            requests.add(request);

        }

        for(Integer id : delayer) {

            try {
                stubs[id].takePhase1(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.TakePhase1Response>(c));

                // Exception caught
            } catch (StatusRuntimeException e) {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }

        }

        try {
            c.waitUntilAllListsAreRecieved(numServers);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        long n_lists = c.getNumberOfLists();

        List<List<String>> listOfLists = new ArrayList<>();

        for (int i = 0; i < n_lists; i++) {
            List<String> list = c.getLastListAndRemove();
            listOfLists.add(list);
        }

        return listOfLists;
    }

    public void sendTakePhase1ReleaseRequest(){
        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Take Phase 1 Request initialized correctly\n");
        }

        ArrayList<TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest> requests =
                new ArrayList<TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest>();

        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest request =
                    TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest.newBuilder().setClientId(clientId).build();
            requests.add(request);

        }
        for(Integer id : delayer) {
            try {
                stubs[id].takePhase1Release(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse>(c));
            } catch (StatusRuntimeException e) {
                System.out.println("Locks got released before the take operation finished");
                if (debugMode) {
                    System.err.println("DEBUG: TAKE PHASE 1 RELEASE command stopped.\n");
                }
            }
        }
    }

    public String chooseRandomTuple(List<String> intersection) {
        Random random = new Random();
        int randomIndex = random.nextInt(intersection.size());

        //Get random tuple
        String randomTuple = intersection.get(randomIndex);

        return randomTuple;
    }

    public void sendTakePhase2Request(String tuple){
        ResponseCollector c = new ResponseCollector();

        if(debugMode){
            System.err.println("DEBUG: Take Phase 2 Request initialized correctly\n");
        }
        ArrayList<TupleSpacesReplicaXuLiskov.TakePhase2Request> requests =
                new ArrayList<TupleSpacesReplicaXuLiskov.TakePhase2Request>();

        for(int i = 0; i < numServers; i++) {
            //create requests for each server, with the new tuple
            TupleSpacesReplicaXuLiskov.TakePhase2Request request =
                    TupleSpacesReplicaXuLiskov.TakePhase2Request.newBuilder().setClientId(clientId).setTuple(tuple).build();
            requests.add(request);

        }
        for(Integer id : delayer) {
            try {
                stubs[id].takePhase2(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.TakePhase2Response>(c));
            } catch (StatusRuntimeException e) {
                System.out.println("Locks got released before the take operation finished");
                if (debugMode) {
                    System.err.println("DEBUG: TAKE PHASE 2 command stopped.\n");
                }
            }
        }

        try {
            c.waitUntilAllReceived(numServers);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.print("OK\n" + tuple + "\n\n");
    }


    public List<String> sendGetTupleSpacesStateRequest(String qualifier) {

        ResponseCollector c = new ResponseCollector();

        if (targets.isEmpty() || qualifiers.isEmpty()) {
            System.err.println("No server addresses or qualifiers available.");
            return new ArrayList<>(); // Return an empty list or handle accordingly
        }

        int index = this.qualifiers.indexOf(qualifier);

        if(debugMode){
            System.err.println("DEBUG: GetTupleSpaceStateRequest initialized correctly\n");
        }

        TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest request = TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest.newBuilder().build();

        try {

            stubs[index].getTupleSpacesState(request,  new ClientObserver<TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse>(c));
            c.waitUntilAllListsAreRecieved(1);

            System.out.println("OK");
            if (debugMode){
                System.err.println("DEBUG: GetTupleSpacesStateRequest finished correctly\n");
            }
            // Exception caught
        } catch (StatusRuntimeException | InterruptedException e) {
            System.out.println("Caught exception with description: " +
                    ((e instanceof StatusRuntimeException) ? ((StatusRuntimeException) e).getStatus().getDescription() : e.getMessage()));
            }

        // Returns the list
        return c.getLastListAndRemove();
    }


}
