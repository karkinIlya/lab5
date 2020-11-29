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
import akka.stream.javadsl.Source;
import scala.concurrent.Future;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AkkaHttpServer {

    public static final long TIMEOUT_MILLIS = 10000;

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
                                    urlQuery.get("testurl").get(),
                                    Integer.parseInt(urlQuery.get("count").get())
                                    );
                            System.out.println(result);
                            return result;
                        }
                )
                .map(
                        request -> {
                            return Patterns
                                    .ask(cacheActor, request.first(), Duration.ofMillis(TIMEOUT_MILLIS))
                                    .thenCompose(
                                            result -> {
                                                if(isCorrect((int)result)) {
                                                    return CompletableFuture.completedFuture(
                                                            new Pair<>(request.first(), (int)result));
                                                } else {
                                                    return Source
                                                            .single(request)
                                                            .via(
                                                                    Flow.<Pair<String, Integer>>create().mapConcat(
                                                                            p -> new List<>(Collections.nCopies(p.second(), p.first()))
                                                                    )
                                                            )
                                                            .toMat(obj)
                                                            .run(materializer);
                                                }
                                            }
                                    );
                        }
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