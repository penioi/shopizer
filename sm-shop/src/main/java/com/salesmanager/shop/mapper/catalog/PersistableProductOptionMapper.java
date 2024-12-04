package com.salesmanager.shop.mapper.catalog;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.mapper.Mapper;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import org.apache.commons.collections4.CollectionUtils;
import org.checkerframework.checker.nullness.Opt;
import org.jsoup.helper.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PersistableProductOptionMapper implements Mapper<PersistableProductOption, ProductOption> {

    @Autowired
    private LanguageService languageService;


    ProductOptionDescription description(com.salesmanager.shop.model.catalog.product.attribute.ProductOptionDescription description) throws Exception {
        Validate.notNull(description.getLanguage(), "description.language should not be null");
        ProductOptionDescription desc = new ProductOptionDescription();
        desc.setId(null);
        desc.setDescription(description.getDescription());
        desc.setName(description.getName());
        if (description.getId() != null && description.getId().longValue() > 0) {
            desc.setId(description.getId());
        }
        Language lang = languageService.getByCode(description.getLanguage());
        desc.setLanguage(lang);
        return desc;
    }


    @Override
    public ProductOption convert(PersistableProductOption source, MerchantStore store, Language language) {
        ProductOption destination = new ProductOption();
        return merge(source, destination, store, language);
    }


    @Override
    public ProductOption merge(PersistableProductOption source, ProductOption destination,
                               MerchantStore store, Language language) {
        if (destination == null) {
            destination = new ProductOption();
        }
        final ProductOption option = destination;
        if (!CollectionUtils.isEmpty(source.getDescriptions())) {
            Map<String, com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription> existingDescriptions
                    = destination.getDescriptions().stream().collect(Collectors.toMap(d -> d.getLanguage().getCode(), d -> d));
            destination.setDescriptions(source.getDescriptions().stream().map(
                    srcDesc -> {
                        com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription dest =
                                Optional.ofNullable(existingDescriptions.get(srcDesc.getLanguage())).orElse(new com.salesmanager.core.model.catalog.product.attribute.ProductOptionDescription());
                        dest.setDescription(srcDesc.getDescription());
                        dest.setName(srcDesc.getName());
                        dest.setProductOption(option);
                        try {
                            dest.setLanguage(languageService.getByCode(srcDesc.getLanguage()));
                        } catch (ServiceException e) {
                            throw new RuntimeException(e);
                        }
                        return dest;
                    }
            ).collect(Collectors.toSet()));
        }

        destination.setCode(source.getCode());
        destination.setMerchantStore(store);
        destination.setProductOptionSortOrder(source.getOrder());
        destination.setProductOptionType(source.getType());
        destination.setReadOnly(source.isReadOnly());

        return option;
    }

}