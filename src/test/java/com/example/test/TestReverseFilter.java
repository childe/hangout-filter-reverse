package com.example.test;

import java.util.HashMap;
import java.util.Map;

import com.example.filter.Reverse;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.Yaml;

public class TestReverseFilter {
    @Test
    public void testReverseFilter() {
        String c = String.format("%s\n%s\n%s",
                "fields:",
                "  - name",
                "  - value"
        );
        Yaml yaml = new Yaml();
        Map config = (Map) yaml.load(c);
        Assert.assertNotNull(config);

        Reverse reverseFilter = new Reverse(config);

        // Match
        Map event = new HashMap();
        event.put("name", "hello");
        event.put("nothing", "world");

        event = reverseFilter.process(event);
        Assert.assertEquals(event.get("name"), "olleh");
        Assert.assertEquals(event.get("nothing"), "world");
        Assert.assertNull(event.get("tags"));

        //multi level json
        c = String.format("%s\n%s\n%s",
                "fields:",
                "  - name",
                "  - '[metric][value]'"
        );
        yaml = new Yaml();
        config = (Map) yaml.load(c);
        Assert.assertNotNull(config);

        reverseFilter = new Reverse(config);

        // Match
        event = new HashMap();
        event.put("name", "hello");
        event.put("nothing", "world");
        event.put("metric", new HashMap() {{
            this.put("value", 10);
            this.put("value2", "hangout");
        }});

        event = reverseFilter.process(event);
        Assert.assertEquals(event.get("name"), "olleh");
        Map metric = (Map) event.get("metric");
        Assert.assertEquals(metric.size(), 2);
        Assert.assertEquals(metric.get("value"), "01");
        Assert.assertEquals(metric.get("value2"), "hangout");
        Assert.assertNull(event.get("tags"));
    }
}
