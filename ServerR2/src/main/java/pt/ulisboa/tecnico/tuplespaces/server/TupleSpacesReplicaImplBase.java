package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.ArrayList;
import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleSpacesReplicaImplBase extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

    private ServerState server = new ServerState();
    private boolean debugMode = false ;
    public TupleSpacesReplicaImplBase(boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public void put(TupleSpacesReplicaXuLiskov.PutRequest request, StreamObserver<TupleSpacesReplicaXuLiskov.PutResponse> responseObserver) {

        //get tuple sent by client
        String newTuple = request.getNewTuple();

        //Validate the new Tuple, invoking a Server's State method
        boolean isValid = server.tuppleIsValid(newTuple);

        // Invalid tuple name
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());

            if(debugMode)
                System.err.println("DEBUG: Invalid tuple. PUT command failed.\n");
        } else {
                if (debugMode) {
                    System.err.println("DEBUG: Valid tuple. PUT command initialized correctly.\n");
                }
                //Add new tuple to Server
                server.put(newTuple);

                TupleSpacesReplicaXuLiskov.PutResponse response = TupleSpacesReplicaXuLiskov.PutResponse.newBuilder().build();
                // Send a single response through the stream.
                responseObserver.onNext(response);
                // Notify the client that the operation has been completed.
                responseObserver.onCompleted();
                if (debugMode) {
                    System.err.println("DEBUG: PUT command finished correctly\n");
                }
        }

    }


    @Override
    public void read(TupleSpacesReplicaXuLiskov.ReadRequest request, StreamObserver<TupleSpacesReplicaXuLiskov.ReadResponse> responseObserver){
        if(debugMode){
            System.err.println("DEBUG: READ command initialized correctly\n");
        }

        //Gets the pattern given my the request sent by the user respecting the format of the TupleSpacesCentralized.proto .
        String pattern = request.getSearchPattern();

        //Validate the new Tuple, invoking a Server's State method
        boolean isValid = server.tuppleIsValid(pattern);
        
        // Invalid tuple
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());
            if (debugMode) {
                System.err.println("DEBUG: READ command stopped. Tuple name is invalid\n");
            }
        } else {

            // Reads from the server
            String tuple = server.read(pattern);

            TupleSpacesReplicaXuLiskov.ReadResponse response = TupleSpacesReplicaXuLiskov.ReadResponse.newBuilder().setResult(tuple).build();
            //Send a single response through the stream.
            responseObserver.onNext(response);
            //Notify the client that the operation has been completed .
            responseObserver.onCompleted();

            if (debugMode) {
                System.err.println("DEBUG: READ command finished correctly\n");
            }
        }
    }


    @Override
    public void takePhase1(TupleSpacesReplicaXuLiskov.TakePhase1Request request, StreamObserver<TupleSpacesReplicaXuLiskov.TakePhase1Response> responseObserver) {
        if(debugMode){
            System.err.println("DEBUG: TAKE PHASE 1 command initialized correctly\n");
        }

        //Gets the pattern given my the request sent by the user respecting the format of the TupleSpacesCentralized.proto .
        String searchPattern = request.getSearchPattern();
        int clientId = request.getClientId();

        // Checks if the tuple is valid
        boolean isValid = server.tuppleIsValid(searchPattern);

        // Invalid tuple
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());
            if(debugMode){
                System.err.println("DEBUG: TAKE PHASE 1 command stopped. Tuple name is invalid\n");
            }
        // Valid tuple
        } else {

            List<String> matchingTuples = server.takePhase1(searchPattern, clientId);


            //Build response and remove tuple
            TupleSpacesReplicaXuLiskov.TakePhase1Response response =
                    TupleSpacesReplicaXuLiskov.TakePhase1Response.newBuilder().addAllReservedTuples(matchingTuples).build();
            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            if(debugMode){
                System.err.println("DEBUG: TAKE PHASE 1 finished correctly\n");
            }
        }

    }

    @Override
    public void takePhase1Release(TupleSpacesReplicaXuLiskov.TakePhase1ReleaseRequest request,
                                  StreamObserver<TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse> responseObserver) {
        if(debugMode){
            System.err.println("DEBUG: TAKE PHASE 1 RELEASE command initialized correctly\n");
        }

        int clientId = request.getClientId();

        int result = server.takePhase1Release(clientId);

        //Build response and remove tuple
        if (result == 1) {

            TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse response =
                    TupleSpacesReplicaXuLiskov.TakePhase1ReleaseResponse.newBuilder().build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            if (debugMode) {
                System.err.println("DEBUG: TAKE PHASE 1 RELEASE finished correctly\n");
            }
        }
        else {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("There was a problem while releasing the locks!").asRuntimeException());
            if (debugMode)
                System.err.println("DEBUG: TAKE PHASE 1 RELEASE command couldn't release all locks.\n");
        }


    }

    @Override
    public void takePhase2(TupleSpacesReplicaXuLiskov.TakePhase2Request request,
                           StreamObserver<TupleSpacesReplicaXuLiskov.TakePhase2Response> responseObserver){

        if(debugMode){
            System.err.println("DEBUG: TAKE PHASE 2 command initialized correctly\n");
        }

        String tuple = request.getTuple();
        int clientId = request.getClientId();

        int result = server.takePhase2(tuple, clientId);

        //Build response and remove tuple
        if (result == 1) {

            TupleSpacesReplicaXuLiskov.TakePhase2Response response =
                    TupleSpacesReplicaXuLiskov.TakePhase2Response.newBuilder().build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            if (debugMode) {
                System.err.println("DEBUG: TAKE PHASE 2 RELEASE finished correctly\n");
            }
        }
        else {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("There was a problem while releasing the locks!").asRuntimeException());
            if (debugMode)
                System.err.println("DEBUG: TAKE PHASE 2 command couldn't release all locks.\n");
        }

    }

    @Override
    public void getTupleSpacesState(TupleSpacesReplicaXuLiskov.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse> responseObserver) {

        //Get tuples list
        java.util.List<java.lang.String> tuples = server.getTupleSpacesState();

        if(debugMode){
            System.err.println(" DEBUG: Server's list delivered correctly. getTupleSpacesState iniatilized correctly\n");
        }
        TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse response = TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();

        if(debugMode){
            System.err.println("DEBUG: getTupleSpaceState initialized correctly\n");
        }

    }

}
