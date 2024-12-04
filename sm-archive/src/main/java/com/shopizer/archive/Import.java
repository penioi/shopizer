package com.shopizer.archive;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            rows.forEach(r -> {
                if(r.getRowNum() == 1) {
                    // build title row
                    for (Cell cell : r) {
                        titleRow.add(cell.getRawValue());
                    }
                } else{
                    data.put(r.getRowNum(), new SheetRecord());
                    for (Cell cell : r) {
                        data.get(r.getRowNum()).addCell(cell.getColumnIndex(), titleRow.get(cell.getColumnIndex()), cell.getRawValue());
                    }
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
