package com.shopizer.archive;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductOptionService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductOptionSetService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductOptionValueService;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionSet;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValue;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.core.model.common.description.Description;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOptionValue;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionFacade;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionSetFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Optional;
import java.util.Map; 
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductOptionValuesParser implements CatalogParser<PersistableProductOptionValue>{

    @Autowired
    private ProductOptionSetService productOptionSetService;

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductOptionValueService productOptionValueService;


    @Override
    public List<PersistableProductOptionValue> parse(MerchantStore store, Map<Integer, SheetRecord> data) {
        List<PersistableProductOptionValue> ppovList =  data.entrySet().stream().map( entry -> {
            SheetRecord value = entry.getValue();
            String ppovCode = value.get("property") + "_" + value.get("code");
            ProductOptionValue ppov = Optional.ofNullable(productOptionValueService.getByCode(store, ppovCode)).orElse(new ProductOptionValue()) ;
            ppov.setCode(ppovCode);
            ppov.setMerchantStore(store);
            ProductOptionValueDescription description = new ProductOptionValueDescription();
            if(!ppov.getDescriptions().isEmpty()) {
                description = ppov.getDescriptions().iterator().next();
            } else {
                ppov.getDescriptions().add(description);
            }
            description.setName(value.get("name_" + store.getLanguages().get(0).getCode()));
            description.setDescription(value.get("description_" + store.getLanguages().get(0).getCode()));
            description.setTitle(value.get("name_" + store.getLanguages().get(0).getCode()));
            description.setLanguage(store.getLanguages().get(0));
            description.setProductOptionValue(ppov);
            ProductOptionSet optionSet = productOptionSetService.getCode(store, value.get("property"));
            if(optionSet == null) {
                optionSet = new ProductOptionSet();
                optionSet.setCode(value.get("property"));
            }
            ProductOption productOption = productOptionService.getByCode(store, value.get("property"));
            if(productOption == null) {
                log.error("Failed to find productOption with code {}", value.get("property"));
            } else {
                try {
                    productOptionValueService.save(ppov);
                    optionSet.getValues().add(ppov);
                    optionSet.setOption(productOption);
                    optionSet.setStore(store);
                    productOptionSetService.save(optionSet);
                } catch (ServiceException e) {
                    throw new RuntimeException(e);
                }
            }
            return new PersistableProductOptionValue();
        }).collect(Collectors.toList());


        return ppovList;
    }
    @Override
    public String getEntityName() {
        return "property_value";
    }
}
