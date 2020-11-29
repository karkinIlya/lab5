package akkaHttp;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.Query;
import akka.japi.Pair;
import akka.pattern.Patterns;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import scala.concurrent.Future;
import static org.asynchttpclient.Dsl.asyncHttpClient;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AkkaHttpServer {

    public static final long TIMEOUT_MILLIS = 10000;
    public static final int EMPTY_VALUE = 0;
    public static final int PARALLEL_FLOWS = 1;
    public static final String URL_FIELD = "testurl";
    public static final String COUNT_FIELD = "count";

    public static void main(String[] args) throws IOException {
        System.out.println("start!");
        ActorSystem system = ActorSystem.create("routes");
        final Http http = Http.get(system);
        final ActorMaterializer materializer =
                ActorMaterializer.create(system);
        ActorRef actor = system.actorOf(Props.create(CacheActor.class));
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(actor, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(
                routeFlow,
                ConnectHttp.toHost("localhost", 8080),
                materializer
        );
        System.out.println("Server online at http://localhost:8080/\nPress RETURN to stop...");
        System.in.read();
        binding
                .thenCompose(ServerBinding::unbind)
                .thenAccept(unbound -> system.terminate());
    }

    private static boolean isCorrect(int res) {
        return res >= 0;
    }

    private static int calcDeltaTime(long begin) {
        return (int)(System.currentTimeMillis() - begin);
    }

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(ActorRef cacheActor,
                                                                       ActorMaterializer materializer) {
        // PS. Пока я отходил сеть пропала и gitwatch отвалился. Заметил только сейчас :((
        // При необходимости могу показать локальную историю (не знаю как выгрузить ее в гит)
        return Flow
                .of(HttpRequest.class)
                .map(
                        request -> {
                            Query urlQuery = request.getUri().query();
                            Pair<String, Integer> result = new Pair<>(
                                    urlQuery.get(URL_FIELD).get(),
                                    Integer.parseInt(urlQuery.get(COUNT_FIELD).get())
                                    );
                            System.out.println(result);
                            return result;
                        }
                )
                .mapAsync(
                        PARALLEL_FLOWS,
                        request -> Patterns
                                .ask(cacheActor, request.first(), Duration.ofMillis(TIMEOUT_MILLIS))
                                .thenCompose(
                                        result ->
                                                isCorrect((int)result)
                                                        ? CompletableFuture.completedFuture(
                                                                new Pair<>(request.first(), (int)result))
                                                        : Source
                                                        .single(request)
                                                        .via(
                                                                Flow
                                                                        .<Pair<String, Integer>>create()
                                                                        .mapConcat(
                                                                                p -> new Vector<>(
                                                                                        Collections.nCopies(
                                                                                                p.second(),
                                                                                                p.first()
                                                                                        )
                                                                                )
                                                                        ).mapAsync(
                                                                        request.second(), url -> {
                                                                            long beginTime = System
                                                                                    .currentTimeMillis();
                                                                            System.out.println(beginTime);
                                                                            asyncHttpClient()
                                                                                    .prepareGet(url)
                                                                                    .execute();
                                                                            int deltaTime = calcDeltaTime(beginTime);
                                                                            System.out.println(deltaTime);
                                                                            return CompletableFuture
                                                                                    .completedFuture(
                                                                                            deltaTime
                                                                                    );
                                                                        })
                                                        )
                                                        .toMat(
                                                                Sink.fold(
                                                                        EMPTY_VALUE,
                                                                        Integer::sum
                                                                ),
                                                                Keep.right()
                                                        )
                                                        .run(materializer)
                                                        .thenApply(
                                                                elem -> new Pair<>(
                                                                        request.first(),
                                                                        elem / request.second()
                                                                )
                                                        )
                                )
                )
                .map(
                        request -> {
                            cacheActor.tell(
                                    new CacheMessage(request.first(), request.second()), ActorRef.noSender());
                            return HttpResponse.create().withEntity(request.second().toString().concat("\n"));
                        }
                );
    }
}