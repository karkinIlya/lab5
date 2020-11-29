package akkaJsTests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class JsTest {
    private String packageId;
    private String jsCode;
    private String functionName;
    private String testName;
    private String expectedResult;
    private Integer[] params;
    private String result;

    @JsonCreator
    public JsTest(@JsonProperty("testName")String testName, @JsonProperty("expectedResult")String expectedResult,
                       @JsonProperty("params")Integer[] params) {
        this.testName = testName;
        this.expectedResult = expectedResult;
        this.params = params;
    }

    public String getPackageId() {
        return packageId;
    }

    public String getJsCode() {
        return jsCode;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getTestName() {
        return testName;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public Integer[] getParams() {
        return params;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public void setJsCode(String jsCode) {
        this.jsCode = jsCode;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
}
