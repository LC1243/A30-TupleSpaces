package pt.ulisboa.tecnico.tuplespaces.client.grpc;

import com.google.protobuf.LazyStringArrayList;
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

public class ClientService {

    private ArrayList<String> targets = new ArrayList<String>();

    private boolean debugMode = false;
    OrderedDelayer delayer;

    private int numServers;

    private ManagedChannel[] channels;
    private TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[] stubs;

    public ClientService(boolean debugMode, int numServers) {

        /* TODO: create channel/stub for each server */

        /* The delayer can be used to inject delays to the sending of requests to the
            different servers, according to the per-server delays that have been set  */
        this.numServers = numServers;
        delayer = new OrderedDelayer(numServers);
        this.debugMode = debugMode;
        this.lookupServer();

        channels = new ManagedChannel[numServers];
        stubs = new TupleSpacesReplicaGrpc.TupleSpacesReplicaStub[numServers];

        for (int i = 0; i < numServers; i++) {
            channels[i] = ManagedChannelBuilder.forTarget(targets.get(i)).usePlaintext().build();
            stubs[i] = TupleSpacesReplicaGrpc.newStub(channels[i]);
        }
    }

    /* This method allows the command processor to set the request delay assigned to a given server */
    public void setDelay(int id, int delay) {
        delayer.setDelay(id, delay);

        /* TODO: Remove this debug snippet */
        //System.out.println("[Debug only]: After setting the delay, I'll test it");
        //for (Integer i : delayer) {
        //    System.out.println("[Debug only]: Now I can send request to stub[" + i + "]");
        //}
        //System.out.println("[Debug only]: Done.");
    }

    public void closeChannels() {
        if(debugMode)
            System.err.println("DEBUG: Shutting down channels\n");

        for (ManagedChannel ch : channels)
            ch.shutdown();
    }


    // Lookup for a Server that can satisfy the service TupleSpace, by default with qualifier A
    public void lookupServer() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:5001").usePlaintext().build();
        try {
            NameServerServiceGrpc.NameServerServiceBlockingStub stub = NameServerServiceGrpc.newBlockingStub(channel);
            NameServer.LookupRequest request = NameServer.LookupRequest.newBuilder().setService("TupleSpace").build();

            // Get the ip and port where the server is running
            NameServer.LookupResponse response = stub.lookup(request);

            com.google.protobuf.ProtocolStringList servers = new LazyStringArrayList();
            servers = response.getServerList();

            if (!servers.isEmpty()) {
                for(int i = 0; i < response.getServerCount(); i++) {
                    String target = response.getServer(i);
                    targets.add(target);
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
                stubs[id].read(requests.get(id), new ClientObserver<TupleSpacesReplicaXuLiskov.ReadResponse>(c));

                // Exception caught
            } catch (StatusRuntimeException e) {
                System.out.println("Caught exception with description: " +
                        e.getStatus().getDescription());
            }

            if(!c.getCollectedResponses().isEmpty()){
                System.out.println("OK " + id );
                System.out.println(c.getFirstCollectedResponse());
                return;
            }

        }

        //wait for the first response
        try {
            c.waitUntilAllReceived(1);
            System.out.println("OK");
            System.out.println(c.getFirstCollectedResponse());
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (debugMode) {
            System.err.println("DEBUG: Read Request finished correctly\n");
        }

    }


}
