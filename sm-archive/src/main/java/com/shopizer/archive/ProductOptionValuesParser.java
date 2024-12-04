package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductOptionValuesParser implements CatalogParser<PersistableProductOptionValue>{

    @Override
    public List<PersistableProductOptionValue> parse(MerchantStore store, Map<Integer, SheetRecord> data) {
        return data.entrySet().stream().map( entry -> {
            SheetRecord value = entry.getValue();
            PersistableProductOptionValue ppov = new PersistableProductOptionValue();
            ppov.setId(null);
            ppov.setCode(value.get("code"));
            ppov.setDescriptions(store.getLanguages().stream().filter(l -> value.isSet("description_" + l.getCode()))
                    .map(l -> {
                        ProductOptionValueDescription description = new ProductOptionValueDescription();
                        description.setName(value.get("description_" + l.getCode()));
                        description.setLanguage(l.getCode());
                        return description;
                    }).collect(Collectors.toList()));
            return ppov;
        }).collect(Collectors.toList());
    }
    @Override
    public String getEntityName() {
        return "property_value";
    }
}
