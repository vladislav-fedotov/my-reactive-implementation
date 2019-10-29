package org.test.reactive;


import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
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

        int requestNumberOfElements = 5;
        Long[] array = LongStream.range(0, requestNumberOfElements).boxed().toArray(Long[]::new);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);
        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                signalsOrderKeeper.add(ON_SUBSCRIBE);
                subscription.request(requestNumberOfElements);
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

        assertThat(waitForOnComplete.await(1, SECONDS)).isTrue();

        assertThat(sink).containsExactly(array);
        assertThat(signalsOrderKeeper).containsExactly(ON_SUBSCRIBE, ON_NEXT, ON_COMPLETE);
    }

    @Test
    public void mustSupportBackPressure() throws InterruptedException {
        CountDownLatch waitForOnComplete = new CountDownLatch(1);

        ArrayList<Long> sink = new ArrayList<>();

        int requestNumberOfElements = 5;
        Long[] array = LongStream.range(0, requestNumberOfElements).boxed().toArray(Long[]::new);
        ArrayPublisher<Long> publisher = new ArrayPublisher<>(array);

        final Subscription[] subscriptionKeeper = new Subscription[1];
        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscriptionKeeper[0] = subscription;
            }

            @Override
            public void onNext(Long l) {
                sink.add(l);
            }

            @Override
            public void onError(Throwable t) { }

            @Override
            public void onComplete() {
                waitForOnComplete.countDown();
            }
        });

        assertThat(sink).isEmpty();

        subscriptionKeeper[0].request(1); // Ask for the first element
        assertThat(sink).containsExactly(0L);

        subscriptionKeeper[0].request(1); // Ask for the second element
        assertThat(sink).containsExactly(0L, 1L);

        subscriptionKeeper[0].request(1); // Ask for the third element
        assertThat(sink).containsExactly(0L, 1L, 2L);

        subscriptionKeeper[0].request(1); // Ask for the fourth element
        assertThat(sink).containsExactly(0L, 1L, 2L, 3L);

        subscriptionKeeper[0].request(1); // Ask for the fifth element
        assertThat(sink).containsExactly(0L, 1L, 2L, 3L, 4L);

        assertThat(waitForOnComplete.await(1, SECONDS)).isTrue();
        assertThat(sink).containsExactly(array);
    }

    @Test
    public void mustRaiseExceptionInCaseOfNull() throws InterruptedException {
        CountDownLatch waitForOnComplete = new CountDownLatch(1);

        ArrayList<Long> sink = new ArrayList<>();

        ArrayPublisher<Long> publisher = new ArrayPublisher<>(new Long[] {null, 1L, 2L, 3L, 4L});
        AtomicReference<Throwable> thrownException = new AtomicReference<>();
        AtomicReference<Boolean> onCompleteCalled = new AtomicReference<>(false);
        publisher.subscribe(new Subscriber<Long>() {
            @Override
            public void onSubscribe(Subscription subscription) {
                subscription.request(5);
            }

            @Override
            public void onNext(Long l) {
                sink.add(l);
            }

            @Override
            public void onError(Throwable t) {
                thrownException.set(t);
                waitForOnComplete.countDown();
            }

            @Override
            public void onComplete() {
                onCompleteCalled.set(true);
            }
        });

        assertThat(waitForOnComplete.await(1, SECONDS)).isTrue();

        assertThat(thrownException.get()).isNotNull();
        assertThat(thrownException.get()).isInstanceOf(NullPointerException.class);
        assertThat(sink).isEmpty();
        assertThat(onCompleteCalled.get()).isFalse();
    }
}