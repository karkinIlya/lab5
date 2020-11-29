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

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class AkkaHttpServer {

    public static final int TIMEOUT_MILLIS = 10000;

    public static void main(String[] args) throws IOException {
        System.out.println("start!");
        ActorSystem system = ActorSystem.create("routes");
        final Http http = Http.get(system);
        final ActorMaterializer materializer =
                ActorMaterializer.create(system);
        ActorRef actor = system.actorOf(Props.create(CacheActor.class));
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createFlow(http, actor);
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

    private static Flow<HttpRequest, HttpResponse, NotUsed> createFlow(Http http, ActorRef cacheActor) {
        // PS Я зн
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
                            Patterns.ask(cacheActor, request.first(), TIMEOUT_MILLIS).andThen(
                                    result -> {

                                    }
                            )
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