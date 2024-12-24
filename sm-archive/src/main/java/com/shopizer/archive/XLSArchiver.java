package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.model.entity.Entity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
public class XLSArchiver {

    @Inject
    private Import anImport;

    @Inject
    private List<CatalogParser<?>> parsers;

    //@Value("${archive.upload.path}")
    private String directory;

    public static void writeFileFromInputStream(InputStream inputStream, String outputFilePath) {
        try (FileOutputStream outputStream = new FileOutputStream(outputFilePath)) {
            byte[] buffer = new byte[1024]; // Buffer size for reading chunks of data
            int bytesRead;

            // Read from the input stream and write to the output stream
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            System.out.println("File written successfully to: " + outputFilePath);
        } catch (IOException e) {
            System.err.println("Error occurred while writing the file: " + e.getMessage());
        }
    }

    public Map<String, List<? extends Entity>> parseXls(MerchantStore store, InputStream in) throws IOException {
        Map<String, Map<Integer, SheetRecord>> xlsData = anImport.readXls(in);
        Map<String, List<? extends Entity>> result = new TreeMap<>();
        xlsData.entrySet().stream().filter(e -> findParser(e.getKey()) != null).forEach(e -> {
            List<? extends Entity> catalogObjects = findParser(e.getKey()).parse(store, e.getValue());
            result.put(e.getKey(), catalogObjects);
        });
        return result;
    }

    public Map<String, List<? extends Entity>> parseXls(MerchantStore store, File f) throws IOException {
        return parseXls(store, new FileInputStream(f));
    }

    private CatalogParser<?> findParser(String name) {
        return parsers.stream().filter(p -> p.getEntityName().equals(name)).findFirst().orElse(null);
    }

}
