package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionDescription;
import org.checkerframework.checker.nullness.Opt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProductOptionsParser implements CatalogParser<PersistableProductOption>{

    @Override
    public List<PersistableProductOption> parse(MerchantStore store, Map<Integer, SheetRecord> data) {
        return data.entrySet().stream().map( entry -> {
            SheetRecord value = entry.getValue();
            PersistableProductOption ppo = new PersistableProductOption();
            ppo.setId(null);
            ppo.setType(value.get("type"));
            ppo.setOrder(Optional.ofNullable(value.getInteger("order")).orElse(0));
            ppo.setCode(value.get("code"));
            ppo.setReadOnly(value.getBoolean("readonly"));
            ppo.setDescriptions(store.getLanguages().stream().filter(l -> value.hasValue("description_" + l.getCode()))
                    .map(l -> {
                        ProductOptionDescription description = new ProductOptionDescription();
                        description.setName(value.get("description_" + l.getCode()));
                        description.setLanguage(l.getCode());
                        return description;
                    }).collect(Collectors.toList()));
            return ppo;
        }).collect(Collectors.toList());
    }

    @Override
    public String getEntityName() {
        return "property";
    }

}
