package akkaJsTests;

import akka.actor.AbstractActor;
import akka.actor.UntypedActor;

import java.util.*;

public class StoreActor extends UntypedActor {
    private Map<String, List<JsTest>> store = new HashMap<>();

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof JsTest) {
            JsTest test = (JsTest)message;
            if (store.get(test.getPackageId()).isEmpty()) {
                store.put(test.getPackageId(), new ArrayList<>());
            }
            store.get(test.getPackageId()).add(test);
        } else if (message instanceof String) {
            String packageId = (String) message;
            getContext().sender().tell(store.get(packageId).stream().map(s -> s.getResult()), this.getSelf());
        }
    }
}
