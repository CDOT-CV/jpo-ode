package us.dot.its.jpo.ode.udp.map;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TestCase {

    public String description;
    public String input;
    public String expected;

    public static List<TestCase> deserializeTestCases(String path) throws IOException {
        List<TestCase> cases = new ArrayList<>();
        File file = new File(path);
        byte[] jsonData = Files.readAllBytes(file.toPath());
        JSONObject jsonObject = new JSONObject(new String(jsonData));

        JSONArray jsonArray = jsonObject.getJSONArray("cases");

        for (int i = 0; i < jsonArray.length(); i++) {
            TestCase testCase = new TestCase();
            JSONObject json = jsonArray.getJSONObject(i);

            testCase.setDescription(json.getString("description"));

            JSONObject input = json.getJSONObject("input");
            testCase.setInput("\u0000\u0012" + input.toString()); // Add the 2-byte length prefix to the input

            JSONObject expected = json.getJSONObject("expected");
            testCase.setExpected(expected.toString());

            cases.add(testCase);
        }
        return cases;
    }
}
