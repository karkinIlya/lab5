package akkaJsTests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PostPackage {
    private int packageId;
    private String jsScript;
    private String functionName;
    private JsTest[] tests;

    @JsonCreator
    public PostPackage(@JsonProperty("packageId")Integer packageId, @JsonProperty("jsScript")String jsScript,
                       @JsonProperty("functionName")String functionName, @JsonProperty("tests")JsTest[] tests) {
        this.packageId = packageId;
        this.jsScript = jsScript;
        this.functionName = functionName;
        this.tests = tests;
    }

    public int getPackageId() {
        return packageId;
    }

    public String getJsScript() {
        return jsScript;
    }

    public String getFunctionName() {
        return functionName;
    }

    public JsTest[] getTests() {
        return tests;
    }
}
