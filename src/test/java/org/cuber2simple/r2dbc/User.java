package org.cuber2simple.r2dbc;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.ThreadUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Data
@Builder
public class User {

    private String name;

    private Mono<Integer> age;

    public static void main(String[] args) throws Exception {
        Mono<User> userMono = Mono.create(userMonoSink -> {
            userMonoSink.success(
                    User.builder().name("cuber").age(Mono.create(integerMonoSink -> {
                try {
                    ThreadUtils.sleep(Duration.ofSeconds(2l));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                        integerMonoSink.success(23);
                    })).build());
        });

        Mono<UserPure>  mono = userMono.flatMap(user -> user.age)
                .zipWith(userMono).map(t->UserPure.builder().name(t.getT2().name).age(t.getT1()).build());
        CountDownLatch countDownLatch = new CountDownLatch(1);
//        userMono.doOnNext(user -> System.out.println(user)).doFinally(signalType -> countDownLatch.countDown()).subscribe();
        mono.doOnNext(userPure -> userPure.setAge(24)).doFinally(signalType -> countDownLatch.countDown()).subscribe(userPure -> System.out.println(userPure));
        countDownLatch.await(4, TimeUnit.SECONDS);
    }
}
