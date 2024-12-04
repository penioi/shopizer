package com.shopizer.archive;


import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@ToString
public class SheetRecord {

    private final Map<Integer, RecordCell> row = new TreeMap<>();
    private final Map<String, RecordCell> rowByName = new TreeMap<>();
    public void addCell(int index, String name, String value) {
        this.row.put(index, RecordCell.builder().index(index).name(name).value(value).build());
        this.rowByName.put(name, RecordCell.builder().index(index).name(name).value(value).build());
    }

    public String get(String name) {
        return Optional.ofNullable(this.rowByName.get(name)).map(RecordCell::getValue).map(String::trim).orElse(null);
    }

    public Integer getInteger(String name) {
        return Optional.ofNullable(this.rowByName.get(name)).map(RecordCell::getValue).map(String::trim).map(Integer::valueOf).orElse(null);
    }

    public boolean getBoolean(String name) {
        return Optional.ofNullable(this.rowByName.get(name))
                .map(RecordCell::getValue)
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
        return this.row.get(index).getValue();
    }

    public boolean isSet(String s) {
        return get(s) != null;
    }

    @Data
    @Builder
    @ToString
    public static class RecordCell {
        private int index;
        private String name;
        private String value;
    }

}
