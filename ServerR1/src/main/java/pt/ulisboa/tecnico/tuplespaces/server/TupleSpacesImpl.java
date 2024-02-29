package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.centralized.contract.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
import static io.grpc.Status.INVALID_ARGUMENT;

public class TupleSpacesImpl extends TupleSpacesGrpc.TupleSpacesImplBase {

    private ServerState server = new ServerState();
    private boolean debugMode = false ;
    public TupleSpacesImpl(boolean debugMode) {
        this.debugMode = debugMode;
    }
    @Override
    public void put(TupleSpacesCentralized.PutRequest request, StreamObserver<TupleSpacesCentralized.PutResponse> responseObserver) {

        //get tuple sent by client
        String newTuple = request.getNewTuple();

        //Validate the new Tuple, invoking a Server's State method
        boolean isValid = server.tuppleIsValid(newTuple);
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

                TupleSpacesCentralized.PutResponse response = TupleSpacesCentralized.PutResponse.newBuilder().build();
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
    public void read(TupleSpacesCentralized.ReadRequest request, StreamObserver<TupleSpacesCentralized.ReadResponse> responseObserver){
        if(debugMode){
            System.err.println("DEBUG: READ command initialized correctly\n");
        }
        //Gets the pattern given my the request sent by the user respecting the format of the TupleSpacesCentralized.proto .
        String pattern = request.getSearchPattern();
        // Reads from the server
        String tuple = server.read(pattern);

        TupleSpacesCentralized.ReadResponse response = TupleSpacesCentralized.ReadResponse.newBuilder().setResult(tuple).build();
        //Send a single response through the stream.
        responseObserver.onNext(response);
        //Notify the cliente that the operation has been completed .
        responseObserver.onCompleted();

        if(debugMode){
            System.err.println("DEBUG: READ command finished correctly\n");
        }
    }

    @Override
    public void take(TupleSpacesCentralized.TakeRequest request, StreamObserver<TupleSpacesCentralized.TakeResponse> responseObserver) {
        if(debugMode){
            System.err.println("DEBUG: TAKE command initialized correctly\n");
        }
        //Gets the pattern given my the request sent by the user respecting the format of the TupleSpacesCentralized.proto .
        String searchPattern = request.getSearchPattern();

        // Checks if the tuple is valid
        boolean isValid = server.tuppleIsValid(searchPattern);

        if(!isValid) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Tuple Name is Not Valid!").asRuntimeException());
            if(debugMode){
                System.err.println("DEBUG: TAKE command stopped. Tuple name is invalid\n");
            }
        // Valid tuple
        } else {
            //Build response and remove tuple
            TupleSpacesCentralized.TakeResponse response = TupleSpacesCentralized.TakeResponse.newBuilder().setResult(server.take(searchPattern)).build();
            // Send a single response through the stream.
            responseObserver.onNext(response);
            // Notify the client that the operation has been completed.
            responseObserver.onCompleted();
            if(debugMode){
                System.err.println("DEBUG: TAKE finished correctly\n");
            }
        }

    }

    @Override
    public void getTupleSpacesState(TupleSpacesCentralized.getTupleSpacesStateRequest request, StreamObserver<TupleSpacesCentralized.getTupleSpacesStateResponse> responseObserver) {

        //Get tuples list
        java.util.List<java.lang.String> tuples = server.getTupleSpacesState();

        if(debugMode){
            System.err.println(" DEBUG: Server's list delivered correctly. getTupleSpacesState iniatilized correctly\n");
        }
        TupleSpacesCentralized.getTupleSpacesStateResponse response = TupleSpacesCentralized.getTupleSpacesStateResponse.newBuilder().addAllTuple(tuples).build();
        // Send a single response through the stream.
        responseObserver.onNext(response);
        // Notify the client that the operation has been completed.
        responseObserver.onCompleted();

        if(debugMode){
            System.err.println("DEBUG: getTupleSpaceState initialized correctly\n");
        }

    }

}
