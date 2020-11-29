package akkaHttp;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;

import java.util.Map;
import java.util.TreeMap;

public class CacheActor extends AbstractActor {
    private Map<String, Integer> cache = new TreeMap<>();

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(  // GET
                        String.class, message ->
                                this.getSender()
                                        .tell(cache.containsKey(message) ? cache.get(message) : -1, this.getSelf())
                )
                .match(
                        CacheMessage.class, message ->
                                cache.putIfAbsent(message.getUrl(), message.getTime())
                )
                .build();
    }
}
