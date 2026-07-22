package com.toprao.toop.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.toprao.JsonUtils;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

@Getter
public class ToOpN1Definition {
    @JsonProperty("monitored_elements")
    private List<ToOpGridElement> monitoredElements;

    @JsonProperty("contingencies")
    private List<ToOpContingency> contingencies;

    @JsonProperty("id_type")
    private String idType;

    public ToOpN1Definition(List<ToOpGridElement> monitoredElements, List<ToOpContingency> contingencies, String idType) {
        this.monitoredElements = monitoredElements;
        this.contingencies = contingencies;
        this.idType = idType;
    }

    public ToOpN1Definition() {
    }

    public static ToOpN1Definition read(Path filePath) {
        try (InputStream is = Files.newInputStream(filePath)) {
            return readFromInputStream(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ToOpN1Definition readFromInputStream(InputStream is) {
        Objects.requireNonNull(is);
        try {
            return JsonUtils.getObjectMapper().readValue(is, ToOpN1Definition.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public ToOpGridElement getMonitoredElement(String monitoredElementId) {
        // TODO add index by id
        return monitoredElements.stream().filter(c -> c.getId().equals(monitoredElementId)).findFirst().orElse(null);
    }

    public ToOpContingency getContingency(String contingencyId) {
        // TODO add index by id
        return contingencies.stream().filter(c -> c.getId().equals(contingencyId)).findFirst().orElse(null);
    }

    public void write(Path p) {
        try {
            JsonUtils.getObjectMapper().writeValue(p.toFile(), this);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
