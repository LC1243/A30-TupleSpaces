package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.*;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase1Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.TakePhase2Response;
import pt.ulisboa.tecnico.tuplespaces.replicaXuLiskov.contract.TupleSpacesReplicaXuLiskov.getTupleSpacesStateResponse;

import java.util.List;


public class ClientObserver<R> implements StreamObserver<R>  {

    ResponseCollector collector;

    public ClientObserver (ResponseCollector c) {
        collector = c;
    }

    @Override
    public void onNext(R response) {
        if (response instanceof PutResponse) {
            PutResponse putResponse = (PutResponse) response;
            collector.addString("OK");

        } else if (response instanceof ReadResponse) {
            ReadResponse readResponse = (ReadResponse) response;
            collector.addString(readResponse.getResult());

        } else if (response instanceof TakePhase1Response) {
            // Handle TakePhase1Response
            TakePhase1Response takePhase1Response = (TakePhase1Response) response;

            List<String> matchingTuples = takePhase1Response.getReservedTuplesList();

            if(collector.getCollectedResponses().isEmpty()) {
                collector.addAllStrings(matchingTuples, true);
                return;
            }

            collector.addAllStrings(matchingTuples, false);

        } else if (response instanceof TakePhase1ReleaseResponse) {
            // Handle TakePhase2Response

        } else if (response instanceof  TakePhase2Response) {

        }
            /*
        } else if (response instanceof getTupleSpacesStateResponse) {
            // Handle getTupleSpacesStateResponse
        } else {
            // Handle other types of responses
        }*/
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {}

}
