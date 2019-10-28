package org.test.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Reactive streams [specification|https://github.com/reactive-streams/reactive-streams-jvm#specification]
 * @param <T>
 */
public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        subscriber.onSubscribe(new Subscription() {
            @Override
            public void request(long n) {

            }

            @Override
            public void cancel() {

            }
        });
        for (int i = 0; i < array.length; i++) {
            subscriber.onNext(array[i]);
        }
        subscriber.onComplete();
    }
}
