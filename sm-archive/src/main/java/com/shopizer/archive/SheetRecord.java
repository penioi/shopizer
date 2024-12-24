package com.shopizer.archive;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

@ToString
public class SheetRecord {

    private final Map<Integer, RecordCell> row = new TreeMap<>();
    private final Map<String, RecordCell> rowByName = new TreeMap<>();

    public void addCell(int index, String name, String value) {
        if(this.rowByName.containsKey(name)) {
            this.rowByName.get(name).addValue(value);
        } else {
            RecordCell cell = RecordCell.builder().index(index).name(name).value(value).build();
            this.row.put(index, cell );
            this.rowByName.put(name, cell);
        }
    }

    public String get(String name) {
        return Optional.ofNullable(this.rowByName.get(name)).map(RecordCell::pop).map(String::trim).orElse(null);
    }

    public Integer getInteger(String name) {
        return Optional.ofNullable(this.rowByName.get(name)).map(RecordCell::pop).map(String::trim).map(Float::valueOf).map(Float::intValue).orElse(null);
    }


    public Double getDecimal(String name) {
        return Optional.ofNullable(this.rowByName.get(name)).map(RecordCell::pop).map(String::trim).map(Double::valueOf).orElse(null);
    }

    public Stream<RecordCell> stream() {
        return this.rowByName.values().stream();
    }

    public boolean getBoolean(String name) {
        return Optional.ofNullable(this.rowByName.get(name))
                .map(RecordCell::pop)
                .map(String::trim)
                .map(String::toLowerCase)
                .map(s -> {
                    switch (s) {
                        case "1" :
                        case "true" :
                        case "y" :
                        case "yes":
                            return Boolean.TRUE;
                        default: return Boolean.FALSE;
                    }
                }).orElse(false);
    }

    public String get(int index) {
        return this.row.get(index).pop();
    }

    public boolean hasValue(String s) {
        RecordCell sr = this.rowByName.get(s);
        return sr != null && sr.hasValue();
    }

    @Data
    @ToString
    @AllArgsConstructor
    public static class RecordCell {
        private int index;
        private String name;
        private List<String> values = new ArrayList<>();
        private int currentIndex;

        public boolean hasValue() {
            return !this.values.isEmpty();
        }

        public String pop() {
            if(this.values.size() == currentIndex) {
                currentIndex = 0;
            }
            if(this.values.isEmpty()) {
                return null;
            }
            return this.values.get(currentIndex++);
        }

        public RecordCell addValue(String value) {
            this.values.add(value);
            return this;
        }

        public static RecordCellBuilder builder() {
            return new RecordCellBuilder();
        }

        public static class RecordCellBuilder {
            private int index;
            private String name;
            private List<String> values = new ArrayList<>();

            public RecordCellBuilder index(int index) {
                this.index = index;
                return this;
            }

            public RecordCellBuilder name(String name) {
                this.name = name;
                return this;
            }

            public RecordCellBuilder value(String value) {
                this.values.add(value);
                return this;
            }

            public RecordCell build() {
                return new RecordCell(this.index, this.name, this.values, 0);
            }
        }

    }

}
