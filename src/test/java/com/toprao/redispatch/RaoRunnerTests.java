package com.toprao.redispatch;

import com.toprao.redispatch.result.ActionSummary;
import com.toprao.redispatch.result.ActionType;
import com.toprao.redispatch.result.CnecSummary;
import com.toprao.redispatch.result.RaoSummary;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class RaoRunnerTests {

    @Test
    void writeRaoSummary(@TempDir Path outputDir) throws IOException {
        OffsetDateTime timestamp = OffsetDateTime.of(2026, 4, 25, 12, 0, 0, 0, ZoneOffset.UTC);
        List<ActionSummary> actions = List.of(new ActionSummary("PST_1_tap_change", "State_CURATIVE", timestamp, ActionType.PST, 10.4, 0.));
        List<CnecSummary> limitingElements = List.of(new CnecSummary("Branch 1", "CO Branch 2", 2.343, "A", "State_CURATIVE", timestamp));

        RaoSummary summary = new RaoSummary(true, 102.029, actions, limitingElements);
        Path summaryPath = outputDir.resolve("summary.json");
        summary.write(summaryPath);
        String json = FileUtils.readFileToString(summaryPath.toFile(), StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("""
              {
                "secure" : true,
                "functional_cost" : 102.03,
                "actions" : [ {
                  "name" : "PST_1_tap_change",
                  "state_id" : "State_CURATIVE",
                  "timestamp" : 1777118400.000000000,
                  "type" : "PST",
                  "value" : 10.40,
                  "cost" : 0.00
                } ],
                "limiting_elements" : [ {
                  "element_name" : "Branch 1",
                  "contingency_name" : "CO Branch 2",
                  "margin" : 2.34,
                  "unit" : "A",
                  "state_id" : "State_CURATIVE",
                  "timestamp" : 1777118400.000000000
                } ]
              }""");
    }

}
