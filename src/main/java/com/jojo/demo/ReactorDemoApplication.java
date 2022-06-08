package com.jojo.demo;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.jojo.demo.domain.ResponseStatusRetry;

import lombok.SneakyThrows;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;

@SuppressWarnings("all")
@SpringBootApplication
public class ReactorDemoApplication {

  public static record O(int val) {}

  // Debug and go through this line by line
  public static void main(String[] args) {

    SpringApplication.run(ReactorDemoApplication.class, args);

    final Flux<Integer> intFlux = Flux.just(1, 2, 3, 4, 5, 6, 7);

    intFlux.subscribe(onNext -> System.out.print(onNext + " "));

    System.out.println();

    intFlux.log().subscribe(onNext -> System.out.print(onNext + " "));

    System.out.println();

    intFlux.log().limitRate(5).subscribe(onNext -> System.out.print(onNext + " "));

    System.out.println();

    Mono.just(2)
        .doOnNext(
            i -> {
              throw new RuntimeException("Error");
            })
        .subscribe(
            onNext -> System.out.println(onNext), onError -> System.err.println(onError + " \n"));

    // subscribe can handle 3 signal types
    intFlux.subscribe(
        onNext -> System.out.print(onNext),
        onError -> System.err.print(onError),
        () -> System.out.print(" Complete Signal Recieved \n"));

    // operators
    intFlux
        .doOnNext(System.out::print)
        .map(Integer::toHexString)
        .doOnComplete(() -> System.out.println(" Flux Complete"))
        .subscribe(onNext -> System.out.print(onNext + " "));

    /* Handling errors */

    // recover with static value
    Mono.error(new Throwable("Error"))
        .onErrorReturn(42)
        .subscribe(System.out::println, System.err::println);

    System.out.println();

    // OnErrorResume recovers with dynamic values by
    // constructing a new Publisher
    Flux.error(new Throwable("Error"))
        .onErrorResume(ex -> Mono.just(56))
        .subscribe(System.out::println, System.err::println);

    /* Retrying errors */

    // retryWhen will resubscribe if conditions are met;
    intFlux
        .doOnNext(
            i -> {
              if (i > 5) throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
            })
        // Checkpoint Adds to StackTrace for Simple Debugging
        .checkpoint("doOnNext Checkpoint")
        .log()
        .retryWhen(ResponseStatusRetry.getRetrySpecification())
        .subscribe(null, ex -> System.err.println("Failed to retry"));

    /* Transforming data */

    // Map alters OnNext signals flowing in the stream
    Flux.just(1, 2, 3, 4, 5, 6, 7)
        .map(i -> i * 10)
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // OnErrorMap alters Error signals flowing in the stream
    Mono.error(new Throwable("Error"))
        .doOnError(System.err::println)
        .onErrorMap(RuntimeException::new)
        .subscribe(null, System.err::println);

    // FlatMap creates internal Publishers and
    // interleaves the emitted elements
    Flux.just(1, 2, 3, 4, 5, 6, 7)
        .flatMap(i -> Flux.just(i, i * 10, i * 100).delayElements(randomDelay()))
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // ConcatMap preserves order of internal Publishers
    Flux.just(1, 2, 3, 4, 5, 6, 7)
        .concatMap(i -> Flux.just(i, i * 10, i * 100).delayElements(randomDelay()))
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // FlatMapSequential Internally Queues elements to preserve order
    Flux.just(1, 2, 3, 4, 5, 6, 7)
        .flatMapSequential(
            onNext -> Flux.just(onNext, onNext * 10, onNext * 100).delayElements(randomDelay()))
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    /* Combining Streams */

    final Mono<Integer> intMono1 = Mono.just(360);

    final Mono<Integer> intMono2 = Mono.just(1024);

    // Merge same Type Publisher
    intMono1
        .mergeWith(intMono2)
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // Merge same Type Publisher Ordered
    intMono1
        .concatWith(intMono2)
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // Merge Different Type Publishers with zip()

    final Mono<String> strMono = Mono.just("String");

    Mono.zip(intMono1, strMono)
        .subscribe(
            tuple2 ->
                System.out.println("Combined Response: " + tuple2.getT1() + " " + tuple2.getT2()));

    System.out.println();

    /* TupleUtils can be useful to avoid calling multiple get functions */
    Mono.zip(intMono1, intMono2, strMono)
        .subscribe(
            TupleUtils.consumer(
                (t1, t2, t3) ->
                    System.out.println("Combined Response: " + t1 + " " + t2 + " " + t3)));

    System.out.println();

    // tupleUtils with an operator
    final Mono<String> combinedMono =
        Mono.zip(intMono1, intMono2, strMono)
            .map(
                TupleUtils.function(
                    (t1, t2, t3) -> "Combined Response: " + t1 + " " + t2 + " " + t3));

    // you can zip up to 8 Publishers at once and keep using tuple
    Flux.zip(
            intFlux,
            intMono1,
            intMono2,
            strMono,
            Mono.just(21),
            Mono.just(new Object()),
            combinedMono,
            Flux.just((double) 223, Double.valueOf(3)))
        .map(TupleUtils.function(ReactorDemoApplication::tuple8Combiner))
        .subscribe(System.out::println);

    /* Repeat a mono on a certain condition */
    Mono.fromSupplier(() -> randomDelay().toMillis())
        // repeatedly re-subscribe to the source Mono (this produces a Flux<Status>)
        .repeatWhen(completed -> completed.delayElements(Duration.ofMillis(1000))) // (1)
        .doOnNext(onNext -> System.out.print(onNext + " "))
        // cancel the repeat cycle as soon as the condition is met
        .filter(i -> i <= 169)
        .next()
        // set cycle to timeout if condition isn't met in Time
        .timeout(Duration.ofMillis(10000))
        .subscribe(System.out::println);

    /* Transform Demonstration */

    // the following two streams Are Identical
    intFlux
        .map(onNext -> onNext * 2)
        .doOnNext(
            i -> {
              throw Exceptions.propagate(new Throwable("Error"));
            })
        .onErrorResume(ex -> Mono.just(69))
        .doOnComplete(System.out::println)
        .subscribe(System.out::print);

    intFlux
        .map(onNext -> onNext * 2)
        // transform is really useful for making code reusable
        .transform(ReactorDemoApplication::transformer)
        .subscribe(System.out::print);

    /*Using Transform to rate Limit a Flux */
    intFlux
        .transform(flux -> limitTPS(flux, 2))
        .doOnComplete(System.out::println)
        .subscribe(onNext -> System.out.print(onNext + " "));

    /* Wrapping a Blocking Operation */
    Mono.fromCallable(() -> StatFuture("id2").get())
        // Tells Mono what Thread Pool it must use to subscribe
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(
            name ->
                System.out.println(
                    "Blocking Operation Completed On Thread " + Thread.currentThread().getName()));

    /* Converting CompletableFuture to a Mono */

    final CompletableFuture<String> future =
        NameFuture("id1")
            .thenApply(
                name -> {
                  System.out.println("Future Complete");
                  return name;
                });

    // this works, but the future is running before the mono is subscribed
    Mono.fromFuture(future).subscribe(System.out::print);

    System.out.println();

    // Here are two ways to correct this behavior

    // (1) use a supplier to lazily construct the Future
    Mono.fromFuture(() -> NameFuture("id2"))
        .doOnSuccess(name -> System.out.println("Future Complete"))
        .subscribe(System.out::print);

    System.out.println();

    // (2) Construct the future in an Operator and Flatmap The Future to a Mono.
    Mono.just("id1")
        .map(ReactorDemoApplication::NameFuture)
        .flatMap(Mono::fromFuture)
        .doOnSuccess(name -> System.out.println("Future Complete"))
        .subscribe(System.out::print);

    System.out.println();

    // shows difference in verbosity between futures and mono
    futureVsReactive();

    /* Caching */

    final Mono<Long> randomLong = Mono.fromSupplier(() -> randomDelay().toMillis());

    // use your imagination and pretend this is redis or something
    final Map<String, Long> cache = new HashMap<>();

    final var key = "key";
    final var cachedMono =
        // lookup value using key
        Mono.defer(() -> Mono.justOrEmpty(cache.get(key)))
            // set source Mono and write into cache on miss
            .switchIfEmpty(
                randomLong
                    //
                    .doOnSuccess(v -> cache.put(key, v)));

    // cache will miss first time because the map is empty
    cachedMono.subscribe(System.out::println);

    cachedMono.subscribe(System.out::println);
    // if we clear the cache it will subscribe to source mono.
    cache.clear();
    cachedMono.subscribe(System.out::println);

    /**
     * onErrorContinue is an Advanced and highly specialized operator for handling errors
     *
     * <p>onErrorContinue Changes Upstream ErrorMode to OnErrorContinue Which Recovers By Dropping
     * incriminating elements from stream (no Error signal will propagate)
     */
    onErrorContinueDemo();
  }

  static String tuple8Combiner(
      Integer t1, Integer t2, Integer t3, String t4, Integer t5, Object t6, String t7, Double t8) {
    return "Combined: " + t1 + t2 + t3 + t4 + t5 + t6 + t7 + t8;
  }

  static Duration randomDelay() {
    return Duration.ofMillis((long) (Math.random() * (500 - 0 + 1)) + 0);
  }

  static Flux<Integer> transformer(Flux<Integer> intflux) {

    return intflux
        .doOnNext(
            i -> {
              throw Exceptions.propagate(new Throwable("Error"));
            })
        .onErrorResume(ex -> Mono.just(69))
        .doOnComplete(System.out::println);
  }

  /**
   * Compares verbosity of Non-Blocking CompletableFuture vs Flux <br>
   * <br>
   * The task is to asynchronously get the name, stat, and card of a given list of ID, combine the
   * results, and print to console
   */
  static void futureVsReactive() {
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println();
    final List<String> idlist = Arrays.asList("id1", "id2");

    Flux.fromIterable(idlist)
        .flatMap(
            id -> {
              final var nameTask = NameMono(id);
              final var statTask = StatMono(id);
              final var cardTask = cardMono(id);

              return Mono.zip(nameTask, statTask, cardTask)
                  .map(
                      TupleUtils.function(
                          (name, stat, card) ->
                              "Name " + name + " has stat " + stat + " and Card " + card));
            })
        .subscribe(System.out::println);

    // vs

    CompletableFuture.supplyAsync(() -> idlist)
        .thenComposeAsync(
            list -> {
              final Stream<CompletableFuture<String>> zip =
                  list.stream()
                      .map(
                          i -> {
                            final var nameTask = NameFuture(i);
                            final var statTask = StatFuture(i);
                            final var cardTask = cardFuture(i);

                            return nameTask
                                .thenCombineAsync(statTask, Tuples::of)
                                .thenCombineAsync(
                                    cardTask,
                                    (nameStatTuple2, card) ->
                                        "Name "
                                            + nameStatTuple2.getT1()
                                            + " has stat "
                                            + nameStatTuple2.getT2()
                                            + " and Card "
                                            + card);
                          });

              final List<CompletableFuture<String>> combinationList =
                  zip.collect(Collectors.toList());
              final CompletableFuture<String>[] combinationArray =
                  combinationList.toArray(new CompletableFuture[combinationList.size()]);

              final var allDone = CompletableFuture.allOf(combinationArray);
              return allDone.thenApply(
                  v ->
                      combinationList.stream()
                          .map(CompletableFuture::join)
                          .collect(Collectors.toList()));
            })
        .thenApply(
            list -> {
              list.forEach(System.out::println);
              return list;
            });
  }

  /**
   * Uses the window operator to limit tps
   *
   * @param flux
   * @param maxTps the tps limit
   * @return
   */
  private static <T> Flux<T> limitTPS(Flux<T> flux, int maxTps) {
    return flux.window(maxTps).delayElements(Duration.ofSeconds(1)).flatMap(window -> window);
  }

  /**
   * onErrorContinue is an Advanced and highly specialized operator for handling errors. Use With
   * Caution
   *
   * <p>OnErrorContinue Changes Upstream ErrorMode to OnErrorContinue Which Recovers By Dropping
   * incriminating elements from stream (no Error signal will propagate)
   */
  private static void onErrorContinueDemo() {

    Flux.just(3, 45, 69, 97, 1, 420, 2)
        .doOnNext(ReactorDemoApplication::nice)
        // this code will never execute because no error signals will come
        .onErrorResume(ex -> Mono.empty())
        // change error mode upstream
        .onErrorContinue(
            (ex, obj) -> {
              System.err.println("\n" + ex + "\n incriminating element: " + obj + "\n");
            })
        .doOnComplete(System.out::println)
        .subscribe(i -> System.out.print(i + " "));

    // OnErrorStop Disables OnErrorContinue Mode Upstream
    // Causing error signal to propagate
    Flux.just(420, 3, 45, 69, 97, 1, 2)
        .doOnNext(
            i -> {
              throw new RuntimeException("Error");
            })
        // All Errors above this operator are propagated as normal
        .onErrorStop()
        // the below onErrorContinue is useless here
        .onErrorContinue(
            (ex, obj) -> {
              System.err.println("\n" + ex + "\n incriminating element: " + obj + "\n");
            })
        .doOnError(System.err::println)
        .onErrorResume(ex -> Flux.empty())
        .doOnComplete(System.out::println)
        .subscribe();

    // there can only be one OnErrorContinue mode active
    Flux.just(420, 3, 45, 69, 97, 1, 2)
        .doOnNext(
            i -> {
              throw new NullPointerException("Error");
            })
        .onErrorContinue(
            InterruptedException.class,
            (ex, obj) -> {
              System.err.println("\n" + ex + "\n incriminating element: " + obj + "\n");
            })
        // this continue mode is overridden
        .onErrorContinue(
            NullPointerException.class,
            (ex, obj) -> {
              System.err.println("\n" + ex + "\n incriminating element: " + obj + "\n");
            })
        .doOnError(ex -> System.err.println("Failed to Continue"))
        .onErrorResume(ex -> Flux.empty())
        .doOnComplete(System.out::println)
        .subscribe();
  }

  // Mono Tasks
  private static Mono<Integer> StatMono(String i) {
    if ("id1".equals(i)) return Mono.just(5);
    return Mono.just(2);
  }

  private static Mono<String> NameMono(String i) {

    if ("id1".equals(i)) return Mono.just("Joseph");
    return Mono.just("Giorno");
  }

  private static Mono<String> cardMono(String i) {

    if ("id1".equals(i)) return Mono.just("Capital One");
    return Mono.just("Chase");
  }

  // future tasks
  private static CompletableFuture<String> NameFuture(String i) {

    if ("id1".equals(i)) return CompletableFuture.supplyAsync(() -> "Joseph");
    return CompletableFuture.supplyAsync(() -> "Giorno");
  }

  private static CompletableFuture<String> cardFuture(String i) {

    if ("id1".equals(i)) return CompletableFuture.supplyAsync(() -> "Capital One");
    return CompletableFuture.supplyAsync(() -> "Chase");
  }

  private static CompletableFuture<Integer> StatFuture(String i) {
    if ("id1".equals(i)) return CompletableFuture.supplyAsync(() -> 5);
    return CompletableFuture.supplyAsync(() -> 2);
  }

  /**
   * Throws error for certain numbers ( ͡° ͜ʖ ͡°)
   *
   * @param i
   */
  @SneakyThrows(InterruptedException.class)
  private static void nice(Integer i) {

    if (i == 69 || i == 420) throw new InterruptedException("Nice");
  }
}
