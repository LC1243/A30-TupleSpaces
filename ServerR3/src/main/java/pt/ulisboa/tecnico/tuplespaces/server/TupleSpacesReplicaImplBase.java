package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;

import java.util.List;

import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleSpacesReplicaImplBase extends TupleSpacesReplicaGrpc.TupleSpacesReplicaImplBase {

    private ServerState server;
    private boolean debugMode = false;
    public TupleSpacesReplicaImplBase(boolean debugMode, String qualifier) {
        this.debugMode = debugMode;
        this.server = new ServerState(debugMode, qualifier);
    }

    @Override
    public void put(TupleSpacesReplicaTotalOrder.PutRequest request,
                    StreamObserver<TupleSpacesReplicaTotalOrder.PutResponse> responseObserver) {

        //Get tuple sent by client
        String newTuple = request.getNewTuple();
        int seqNumber = request.getSeqNumber();
        //FIXME: Deal with sequence number

        // Check if the tuple is valid
        boolean isValid = server.tuppleIsValid(newTuple);

        // Invalid tuple name
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());

            if(debugMode) {
                System.err.println("DEBUG: Invalid tuple. PUT command failed.\n");
            }
        } else {
            if (debugMode) {
                System.err.println("DEBUG: Valid tuple. PUT command initialized correctly.\n");
            }

            //  TODO: FAZER A VERIFICAÇAO DE QUE TAKE_REQUESTS
            //   MAP NAO TEM NENHUMA ENTRADA COM ID IGUAL AO PATTERN

            //Add the tuple to the Server
            server.put(newTuple, seqNumber);

            TupleSpacesReplicaTotalOrder.PutResponse response = TupleSpacesReplicaTotalOrder.PutResponse.newBuilder().build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            if (debugMode) {
                System.err.println("DEBUG: PUT command finished correctly\n");
            }
        }
    }


    @Override
    public void read(TupleSpacesReplicaTotalOrder.ReadRequest request,
                     StreamObserver<TupleSpacesReplicaTotalOrder.ReadResponse> responseObserver){
        if(debugMode){
            System.err.println("DEBUG: READ command initialized correctly\n");
        }

        // Gets the pattern in the TupleSpacesCentralized.proto format
        String pattern = request.getSearchPattern();

        boolean isValid = server.tuppleIsValid(pattern);

        // Invalid tuple
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());
            if (debugMode) {
                System.err.println("DEBUG: READ command stopped. Tuple name is invalid\n");
            }
        } else {
            // Reads from the Server
            String tuple = server.read(pattern);

            TupleSpacesReplicaTotalOrder.ReadResponse response = TupleSpacesReplicaTotalOrder.ReadResponse.newBuilder().setResult(tuple).build();

            //Send a single response through the stream.
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            if (debugMode) {
                System.err.println("DEBUG: READ command finished correctly\n");
            }
        }
    }

    // TODO: Do take function that deals with sequence number

    @Override
    public void take(TupleSpacesReplicaTotalOrder.TakeRequest request,
                    StreamObserver<TupleSpacesReplicaTotalOrder.TakeResponse> responseObserver) {

        //Get tuple sent by client
        String pattern = request.getSearchPattern();
        int seqNumber = request.getSeqNumber();
        //FIXME: Deal with sequence number

        // Check if the tuple is valid
        boolean isValid = server.tuppleIsValid(pattern);

        // Invalid tuple name
        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());

            if(debugMode) {
                System.err.println("DEBUG: Invalid tuple. PUT command failed.\n");
            }
        } else {
            if (debugMode) {
                System.err.println("DEBUG: Valid tuple. PUT command initialized correctly.\n");
            }
            //Remove the tuple from the Server
            String tuple = server.take(pattern, seqNumber);

            TupleSpacesReplicaTotalOrder.TakeResponse response = TupleSpacesReplicaTotalOrder.TakeResponse.newBuilder().setResult(tuple).build();

            // Send a single response through the stream.
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            if (debugMode) {
                System.err.println("DEBUG: PUT command finished correctly\n");
            }
        }
    }


    @Override
    public void getTupleSpacesState(TupleSpacesReplicaTotalOrder.getTupleSpacesStateRequest request,
                                    StreamObserver<TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse> responseObserver) {

        //Get Server's list of tuples
        java.util.List<java.lang.String> tuples = server.getTupleSpacesState();

        if(debugMode){
            System.err.println(" DEBUG: Server's list delivered correctly. getTupleSpacesState initialized correctly\n");
        }

        TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse response = TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();

        // Send a single response through the stream.
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        if(debugMode){
            System.err.println("DEBUG: getTupleSpaceState initialized correctly\n");
        }
    }
}
