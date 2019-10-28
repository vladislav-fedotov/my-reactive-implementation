package org.test.reactive;


import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.LongStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.test.reactive.ArrayPublisherTest.Signal.*;

public class ArrayPublisherTest {

    enum Signal {
        ON_SUBSCRIBE,
        ON_NEXT,
        ON_ERROR,
        ON_COMPLETE
    }

    @Test
    public void signalsShouldBeEmittedInTheRightOrder() throws InterruptedException {
        CountDownLatch waitForOnComplete = new CountDownLatch(1);

        ArrayList<Long> sink = new ArrayList<>();
        ArrayList<Signal> signalsOrderKeeper = new ArrayList<>();

        int toRequest = 5;
        Long[] array = LongStream.range(0, toRequest).boxed().toArray(Long[]::new);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(toRequest);
                signalsOrderKeeper.add(ON_SUBSCRIBE);
            }

            @Override
            public void onNext(Long l) {
                sink.add(l);
                if (!signalsOrderKeeper.contains(ON_NEXT)) {
                    signalsOrderKeeper.add(ON_NEXT);
                }
            }

            @Override
            public void onError(Throwable t) { }

            @Override
            public void onComplete() {
                signalsOrderKeeper.add(ON_COMPLETE);
                waitForOnComplete.countDown();
            }
        });

        waitForOnComplete.await(1, SECONDS);

        Assertions.assertThat(sink).containsExactly(array);
        Assertions.assertThat(signalsOrderKeeper).containsExactly(ON_SUBSCRIBE, ON_NEXT, ON_COMPLETE);
    }
}
