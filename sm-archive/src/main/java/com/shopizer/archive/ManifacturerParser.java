package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ManifacturerParser implements  CatalogParser<PersistableManufacturer>{
    private final Import anImport = new Import();

    public String getEntityName() {
        return "manufacturer";
    }


    public List<PersistableManufacturer> parse(MerchantStore store, Map<Integer, SheetRecord> data)  {
        List<String> languages = store.getLanguages().stream().map(Language::getCode).collect(Collectors.toList());
        Map<String, PersistableManufacturer> manifacturers = new TreeMap<>();
        data.values().forEach(sheetRecord -> {
            log.info("Create category from sheetrecord: {}", sheetRecord);
            String code = sheetRecord.get("code");
            if (!StringUtils.isBlank(code)) {
                //core properties
                PersistableManufacturer manifacturer = new PersistableManufacturer();
                manifacturer.setCode(code);
                
                List<ManufacturerDescription> descriptions = new ArrayList<ManufacturerDescription>();
                for (String language : languages) {
                    if (!sheetRecord.hasValue("name_" + language)) {
                        continue;
                    }
                    ManufacturerDescription description = new ManufacturerDescription();
                    description.setLanguage(language);
                    description.setTitle(sheetRecord.get("name_" + language));
                    description.setName(sheetRecord.get("name_" + language));
                    if (sheetRecord.hasValue("description_" + language)) {
                        description.setDescription(sheetRecord.get("description_" + language));
                    }

                    StringBuilder path = new StringBuilder();
                    String prefix = sheetRecord.get("path_" + language);
                    if (!StringUtils.isBlank(prefix)) {
                        path.append(prefix).append("-");
                    }
                    path.append(anImport.minimalFriendlyUrlCreator(description.getName()));
                    description.setFriendlyUrl(path.toString().toLowerCase());
                    descriptions.add(description);
                }

                manifacturer.setDescriptions(descriptions);
                manifacturers.put(manifacturer.getCode(), manifacturer);
            }
        });
        return new ArrayList<>(manifacturers.values());
    }

}
