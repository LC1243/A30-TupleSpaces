package pt.ulisboa.tecnico.tuplespaces.server;

import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.*;
import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.server.domain.ServerState;
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

}
