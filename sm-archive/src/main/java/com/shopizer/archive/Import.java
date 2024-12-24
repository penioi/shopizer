package com.shopizer.archive;

import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Service;

import com.shopizer.archive.SheetRecord.RecordCell;

import static org.mockito.ArgumentMatchers.eq;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class Import {

    public Map<Integer, SheetRecord> readExcel(InputStream in, String sheetName) throws IOException {
        try (ReadableWorkbook wb = new ReadableWorkbook(in)) {
            Sheet sheet = wb.getSheets().filter(s -> s.getName().equals(sheetName)).findFirst().orElse(null);
            if(sheet == null) {
                return  null;
            }
           return  parseSheet(sheet);
        }
    }

    private  Map<Integer, SheetRecord>  parseSheet(Sheet sheet) throws IOException {
        Map<Integer, SheetRecord> data = new HashMap<>();
        List<String> titleRow = new ArrayList<>();
        try (Stream<Row> rows = sheet.openStream()) {
            rows.filter(r -> r.getPhysicalCellCount() > 0).forEach(r -> {
                if(r.getRowNum() == 1) {
                    // build title row
                    r.stream().filter(Objects::nonNull).forEach(cell -> titleRow.add(cell.getRawValue()));
                } else {
                    data.put(r.getRowNum(), new SheetRecord());
                     r.stream().filter(Objects::nonNull).filter(cell -> cell.getRawValue() != null).filter(cell -> cell.getColumnIndex() < titleRow.size()).forEach( cell -> {
                        data.get(r.getRowNum()).addCell(cell.getColumnIndex(), titleRow.get(cell.getColumnIndex()), cell.getRawValue());
                    });
                }
            });
        }
        return data;
    }

    public Map<String, Map<Integer, SheetRecord>> readXls(InputStream in ) throws IOException {
        Map<String, Map<Integer, SheetRecord>> xlsData = new HashMap<>();
        try (ReadableWorkbook wb = new ReadableWorkbook(in)) {
            wb.getSheets().forEach(sheet ->
                    {
                        try {
                            xlsData.put(sheet.getName(), parseSheet(sheet));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        return  xlsData;
    }

    /**
     * Combine values from two or more consecotive rows if the specified column has no value.
     * Usefull when one column  has more than one value spread on multiple rows
     * @param columnName
     * @param data
     * @return
     */
    public List<SheetRecord> parseExpandedValuesUsing(String columnName,  Map<Integer, SheetRecord> data) {
        List<SheetRecord> res = new ArrayList<>();
        data.entrySet().stream().forEach(entry -> {
            if(entry.getValue().hasValue(columnName)) {
                res.add(new SheetRecord());
            }
            SheetRecord dest = res.get(res.size() - 1);
            SheetRecord toCopy = entry.getValue();
            toCopy.stream().forEach( recordCell -> {
               dest.addCell(recordCell.getIndex(), recordCell.getName(), recordCell.pop());
            }); 
        });
        return res;
    }


    public String minimalFriendlyUrlCreator(String productName) {
        productName = productName.toLowerCase();
        productName = productName.replace("  ", " ");

        //remove accents
        productName = Normalizer.normalize(productName, Normalizer.Form.NFD);
        productName = productName.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");

        productName = productName.replace(" ", "-");
        return productName;
    }


    public static void main(String[] args) throws IOException {
        System.out.println(new Import().readExcel(new FileInputStream("/Users/penio/Downloads/categories.xlsx"), "category"));
    }
}
