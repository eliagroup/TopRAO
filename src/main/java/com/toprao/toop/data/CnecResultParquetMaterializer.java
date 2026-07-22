/*
 * Copyright 2026 50Hertz Transmission GmbH and Elia Transmission Belgium SA/NV
 *
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 * If a copy of the MPL was not distributed with this file,
 * you can obtain one at https://mozilla.org/MPL/2.0/.
 * Mozilla Public License, version 2.0
 */

package com.toprao.toop.data;

import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType.PrimitiveTypeName;
import org.apache.parquet.schema.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CnecResultParquetMaterializer extends RecordMaterializer<ToOpCnecResult> {
    private final ToOpCnecResultGroupConverter root;

    public CnecResultParquetMaterializer(MessageType schema, Class<ToOpCnecResult> recordClass) {
        this.root = new ToOpCnecResultGroupConverter(schema, recordClass);
    }

    @Override public ToOpCnecResult getCurrentRecord() {
        return root.getCurrent();
    }

    @Override public GroupConverter getRootConverter() {
        return root;
    }

    static class FieldSlot {
        final String name;
        final PrimitiveTypeName type;
        Object value;

        FieldSlot(String name, PrimitiveTypeName type) {
            this.name = name; this.type = type;
        }

        void clear() {
            value = null;
        }
    }

    static class ToOpCnecResultGroupConverter extends GroupConverter {
        private final FieldSlot[] slots;
        private final Converter[] converters;
        private final Constructor<ToOpCnecResult> ctor;
        private final String[] ctorParamNames; // index -> name
        private Object currentRecord;

        ToOpCnecResultGroupConverter(MessageType schema, Class<ToOpCnecResult> recordClass) {
            List<Type> fields = schema.getFields();
            this.slots = new FieldSlot[fields.size()];
            this.converters = new Converter[fields.size()];

            for (int i = 0; i < fields.size(); i++) {
                Type t = fields.get(i);
                String name = t.getName();
                PrimitiveTypeName pt = t.asPrimitiveType().getPrimitiveTypeName();
                slots[i] = new FieldSlot(name, pt);
                if (pt == PrimitiveTypeName.DOUBLE) {
                    converters[i] = new DoubleConverter(slots[i]);
                } else if (pt == PrimitiveTypeName.INT32) {
                    converters[i] = new IntConverter(slots[i]);
                } else if (pt == PrimitiveTypeName.INT64) {
                    converters[i] = new LongConverter(slots[i]);
                } else {
                    converters[i] = new StringConverter(slots[i]);
                }
            }

            // find canonical constructor for record and parameter names (cache)
            try {
                Constructor<ToOpCnecResult> found = null;
                for (Constructor<?> c : recordClass.getConstructors()) {
                    if (c.getParameterCount() == fields.size()) {
                        // assume this is the canonical constructor
                        @SuppressWarnings("unchecked")
                        Constructor<ToOpCnecResult> cc = (Constructor<ToOpCnecResult>) c;
                        found = cc;
                        break;
                    }
                }
                if (found == null) {
                    throw new RuntimeException("No matching constructor found for record");
                }
                this.ctor = found;
                Parameter[] params = ctor.getParameters();
                this.ctorParamNames = new String[params.length];
                for (int i = 0; i < params.length; i++) {
                    ctorParamNames[i] = params[i].getName();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override public Converter getConverter(int fieldIndex) {
            return converters[fieldIndex];
        }

        @Override public void start() {
            for (FieldSlot s : slots) {
                s.clear();
            }
        }

        @Override public void end() {
            try {
                // Build args for constructor in parameter order.
                Object[] args = new Object[ctorParamNames.length];

                // Map param name -> slot value (prefer name match; fallback to schema order if missing)
                Map<String, FieldSlot> slotByName = new HashMap<>();
                for (FieldSlot s : slots) {
                    slotByName.put(s.name, s);
                }

                for (int i = 0; i < ctorParamNames.length; i++) {
                    String paramName = ctorParamNames[i];
                    FieldSlot s = slotByName.get(paramName);
                    if (s == null) {
                        // fallback: if parameter not found by name, use slot at same index if exists
                        if (i < slots.length) {
                            s = slots[i];
                        }
                    }
                    if (s == null || s.value == null) {
                        // supply defaults: null for strings, 0.0 / 0 for primitives
                        Class<?> paramType = ctor.getParameterTypes()[i];
                        if (paramType == double.class) {
                            args[i] = 0.0d;
                        } else if (paramType == int.class) {
                            args[i] = 0;
                        } else {
                            args[i] = null;
                        }
                        continue;
                    }

                    // Convert boxed values to expected parameter type
                    Class<?> paramType = ctor.getParameterTypes()[i];
                    if (paramType == double.class || paramType == Double.class) {
                        args[i] = ((Number) s.value).doubleValue();
                    } else if (paramType == int.class || paramType == Integer.class) {
                        long lv = ((Number) s.value).longValue();
                        if (lv < Integer.MIN_VALUE || lv > Integer.MAX_VALUE) {
                            throw new IllegalArgumentException("field " + s.name + " value out of int range: " + lv);
                        }
                        args[i] = (int) lv;
                    } else {
                        args[i] = s.value;
                    }
                }

                currentRecord = ctor.newInstance(args);
            } catch (RuntimeException re) {
                throw re;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public ToOpCnecResult getCurrent() {
            return (ToOpCnecResult) currentRecord;
        }

        // Converters
        static class StringConverter extends PrimitiveConverter {
            private final FieldSlot slot;

            StringConverter(FieldSlot slot) {
                this.slot = slot;
            }

            @Override public void addBinary(Binary value) {
                slot.value = value.toStringUsingUTF8();
            }
        }

        static class DoubleConverter extends PrimitiveConverter {
            private final FieldSlot slot;

            DoubleConverter(FieldSlot slot) {
                this.slot = slot;
            }

            @Override public void addDouble(double value) {
                slot.value = value;
            }
        }

        static class IntConverter extends PrimitiveConverter {
            private final FieldSlot slot;

            IntConverter(FieldSlot slot) {
                this.slot = slot;
            }

            @Override public void addInt(int value) {
                slot.value = value;
            }
        }

        static class LongConverter extends PrimitiveConverter {
            private final FieldSlot slot;

            LongConverter(FieldSlot slot) {
                this.slot = slot;
            }

            @Override public void addLong(long value) {
                slot.value = value;
            }
        }
    }
}
