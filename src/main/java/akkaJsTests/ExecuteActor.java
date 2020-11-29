package akkaJsTests;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.UntypedAbstractActor;
import akka.actor.UntypedActor;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

public class ExecuteActor extends UntypedActor {
    public static final String ENGINE_NAME = "nashorn";

    private String execute(String jsCode, String functionName, Integer[] params) throws ScriptException, NoSuchMethodException {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName(ENGINE_NAME);
        engine.eval(jsCode);
        Invocable invocable = (Invocable) engine;
        return invocable.invokeFunction(functionName, params).toString();
    }

    @Override
    public void onReceive(Object message) throws Throwable {
        if (message instanceof JsTest) {
            JsTest test = (JsTest)message;
            String result = execute(test.getJsCode(), test.getFunctionName(), test.getParams());
            test.setResult(result);
            this.getSender().tell(test, this.getSelf());
        }
    }
}
