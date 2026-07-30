/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.toprao.toop.data.ToOpLfResult;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static <C> C read(Path path, Class<C> clazz) {
        try (InputStream is = Files.newInputStream(path)) {
            return read(is, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <C> C read(InputStream is, Class<C> clazz) {
        try {
            if (clazz.equals(ToOpLfResult.class)) {
                return (C) new ToOpLfResult(JsonUtils.getObjectMapper().readValue(is, new TypeReference<>() { }));
            }
            return JsonUtils.getObjectMapper().readValue(is, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void write(Path p, Object object) {
        try {
            JsonUtils.getObjectMapper().writeValue(p.toFile(), object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(Double.class, new Double2DecimalSerializer());
        module.addSerializer(double.class, new Double2DecimalSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }

    static class Double2DecimalSerializer extends JsonSerializer<Double> {
        @Override
        public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value == null || value.isNaN() || value.isInfinite()) {
                gen.writeNull();
                return;
            }
            BigDecimal bd = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
            gen.writeNumber(bd);
        }
    }
}
