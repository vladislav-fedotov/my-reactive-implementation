package org.test.reactive;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Reactive streams [specification|https://github.com/reactive-streams/reactive-streams-jvm#specification]
 *
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
            int index = 0;
            long alreadyRequestedElements = 0;

            @Override
            public void request(long n) {
                if (alreadyRequestedElements == 0) {
                    alreadyRequestedElements += n;
                    boolean errorOccurred = false;

                    for (; alreadyRequestedElements > 0 && index < array.length && !errorOccurred; alreadyRequestedElements--, index++) {
                        T element = array[index];
                        if (element == null) {
                            subscriber.onError(new NullPointerException());
                            errorOccurred = true;
                        } else {
                            subscriber.onNext(element);
                        }
                    }
                    if (index == array.length && !errorOccurred) {
                        subscriber.onComplete();
                    }
                } else {
                    alreadyRequestedElements += n;
                }
            }

            @Override
            public void cancel() {

            }
        });
    }
}
