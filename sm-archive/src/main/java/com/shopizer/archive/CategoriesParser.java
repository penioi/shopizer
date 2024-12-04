package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.category.CategoryDescription;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
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
public class CategoriesParser implements  CatalogParser<PersistableCategory>{
    private final Import anImport = new Import();
    public String getEntityName() {
        return "category";
    }


    public List<PersistableCategory> parse(MerchantStore store, Map<Integer, SheetRecord> data)  {
        List<String> languages = store.getLanguages().stream().map(Language::getCode).collect(Collectors.toList());
        Map<String, PersistableCategory> categories = new TreeMap<>();
        data.values().forEach(sheetRecord -> {
            log.info("Create category from sheetrecord: {}", sheetRecord);
            String code = sheetRecord.get("code");
            if (!StringUtils.isBlank(code)) {
                //core properties
                PersistableCategory category = new PersistableCategory();
                category.setCode(code);
                category.setSortOrder(Integer.parseInt(sheetRecord.get("position")));
                category.setVisible(Integer.parseInt(sheetRecord.get("visible")) == 1);

                List<CategoryDescription> descriptions = new ArrayList<CategoryDescription>();
                for (String language : languages) {
                    if (!sheetRecord.isSet("name_" + language)) {
                        continue;
                    }
                    CategoryDescription description = new CategoryDescription();
                    description.setLanguage(language);
                    description.setTitle(sheetRecord.get("name_" + language));
                    description.setName(sheetRecord.get("name_" + language));
                    if (sheetRecord.isSet("description_" + language)) {
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

                category.setDescriptions(descriptions);

                categories.put(category.getCode(), category);

                if (!StringUtils.isBlank(sheetRecord.get("parent"))) {
                    PersistableCategory parent = categories.get(sheetRecord.get("parent"));
                    if (parent != null) {
                        Category parentCategory = new Category();
                        parentCategory.setCode(parent.getCode());
                        category.setParent(parentCategory);
                        parent.getChildren().add(category);
                    }
                }
            }
        });
        return new ArrayList<>(categories.values());
    }

}
