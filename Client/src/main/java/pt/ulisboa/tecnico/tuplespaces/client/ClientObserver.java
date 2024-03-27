package pt.ulisboa.tecnico.tuplespaces.client;

import io.grpc.stub.StreamObserver;
import pt.ulisboa.tecnico.sequencer.contract.SequencerOuterClass.GetSeqNumberResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.PutResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.ReadResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.TakeResponse;
import pt.ulisboa.tecnico.tuplespaces.replicaTotalOrder.contract.TupleSpacesReplicaTotalOrder.getTupleSpacesStateResponse;

import java.util.List;


public class ClientObserver<R> implements StreamObserver<R>  {

    ResponseCollector collector;

    public ClientObserver (ResponseCollector c) {
        collector = c;
    }

    @Override
    public void onNext(R response) {
        /* Check the response type */
        if (response instanceof GetSeqNumberResponse) {
            //Successful GetSequenceNumber request
            GetSeqNumberResponse getSeqNumberResponse = (GetSeqNumberResponse) response;
            String seqNumber = Integer.toString(getSeqNumberResponse.getSeqNumber());
            collector.addString(seqNumber);
        }
        else if (response instanceof PutResponse) {
            //Put Request went successfully
            PutResponse putResponse = (PutResponse) response;
            collector.addString("OK");

        } else if (response instanceof ReadResponse) {
            //Read Request successful
            ReadResponse readResponse = (ReadResponse) response;
            collector.addString(readResponse.getResult());

        } else if (response instanceof TakeResponse) {
            //Take Request went sucessfully
            TakeResponse takeResponse = (TakeResponse) response;
            collector.addString(takeResponse.getResult());

        } else if (response instanceof getTupleSpacesStateResponse) {
            // Handle getTupleSpacesStateResponse
            getTupleSpacesStateResponse getTupleSpacesStateResponse = (getTupleSpacesStateResponse) response;

            List<String> tuples = getTupleSpacesStateResponse.getTupleList();

            /* It follows the same behaviour as for delimiting the list with '|',
             * but this time the server doesn't send us his qualifier since we know
             *  which server replied to us
             */
            collector.addAllStrings(tuples);

       }

    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println("Received error: " + throwable);
        collector.setError(true);
    }

    @Override
    public void onCompleted() {}

}
