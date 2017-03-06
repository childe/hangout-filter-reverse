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
    }
}
