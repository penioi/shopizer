package com.salesmanager.shop.store.api.v1.archive;

import com.salesmanager.core.business.repositories.catalog.product.attribute.ProductOptionRepository;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.ProductCriteria;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.product.ReadableProductList;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.ReadableProductAttributeValue;
import com.salesmanager.shop.model.entity.Entity;
import com.salesmanager.shop.store.api.exception.ServiceRuntimeException;
import com.salesmanager.shop.store.api.exception.UnauthorizedException;
import com.salesmanager.shop.store.controller.category.facade.CategoryFacade;
import com.salesmanager.shop.store.controller.manufacturer.facade.ManufacturerFacade;
import com.salesmanager.shop.store.controller.product.facade.ProductFacade;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionFacade;
import com.salesmanager.shop.store.controller.user.facade.UserFacade;
import com.shopizer.archive.XLSArchiver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import pam.shop.model.Attribute;
import springfox.documentation.annotations.ApiIgnore;
import pam.shop.model.SearchProduct;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Controller
@RequestMapping("/api/v1")
@Api(tags = { "Create and restore archived data" })
@SwaggerDefinition(tags = {
        @Tag(name = "Archive data management", description = "Upload/Download store data") })
public class ArchiveApi {

    private static final Logger logger = LoggerFactory.getLogger(ArchiveApi.class);

    private static final int DEFAULT_CATEGORY_DEPTH = 0;

    @Inject
    private XLSArchiver xlsArchiver;

    @Inject
    private CategoryFacade categoryFacade;
    @Inject
    private ProductOptionFacade productOptionFacade;

    @Inject
    private ProductFacade productFacade;

    @Inject
    private ProductOptionRepository productOptionRepository;

    @Inject
    private UserFacade userFacade;

    @Inject
    private ManufacturerService manufacturerService;

    @Inject
    private ManufacturerFacade manufacturerFacade;

    @Inject
    private MerchantStoreService merchantStoreService;


    private final String searchApiUrl = "http://localhost:8082/api/search";

    /**
     * To be used with MultipartFile
     *
     * @param id
     * @param uploadfiles
     * @param request
     * @param response
     * @throws Exception
     */
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = { "/private/archive"}, consumes = {
            MediaType.MULTIPART_FORM_DATA_VALUE }, method = RequestMethod.POST)
    @ApiImplicitParams({ @ApiImplicitParam(name = "store", dataType = "String", defaultValue = "DEFAULT") })
    public void uploadCategories(
            @RequestParam(value = "file", required = true) MultipartFile file,
            @ApiIgnore MerchantStore merchantStore, @ApiIgnore Language language) throws IOException {
        try {
            // superadmin, admin and admin_catalogue
            String authenticatedUser = userFacade.authenticatedUser();
            if (authenticatedUser == null) {
                throw new UnauthorizedException();
            }
            userFacade.authorizedGroup(authenticatedUser, Stream.of(Constants.GROUP_SUPERADMIN, Constants.GROUP_ADMIN, Constants.GROUP_ADMIN_CATALOGUE, Constants.GROUP_ADMIN_RETAIL).collect(Collectors.toList()));

            Map<String, List<? extends Entity>> result =  xlsArchiver.parseXls(merchantStore, file.getInputStream());
            List<PersistableCategory> categories = (List<PersistableCategory>) result.get("category");
            if(categories != null) {
                categories.forEach(c -> {
                    Category existing =  categoryFacade.getByCode(c.getCode(), merchantStore);
                    if(existing != null) {
                        c.setId(existing.getId());
                    }
                    categoryFacade.saveCategory(merchantStore, c);
                });
            }

            List<PersistableProductOption>  productOptions = (List<PersistableProductOption>) result.get("property");
            Optional.ofNullable(productOptions).orElse(new ArrayList<>()).forEach(option -> {
                ProductOption existing = productOptionRepository.findByCode(merchantStore.getId(), option.getCode());
                if(existing != null) {
                    option.setId(existing.getId());
                }
                productOptionFacade.saveOption(option, merchantStore);
            });

            List<PersistableManufacturer> manufacturers = (List<PersistableManufacturer>) result.get("manufacturer");
            Optional.ofNullable(manufacturers).orElse(new ArrayList<>()).forEach( m -> {
                Manufacturer existing = manufacturerService.getByCode(merchantStore, m.getCode());
                if(existing != null) {
                    m.setId(existing.getId());
                }
                try {
                    manufacturerFacade.saveOrUpdateManufacturer(m, merchantStore, null);
                } catch (Exception e) {
                   logger.warn("Error saving manufacturer {}", m, e);
                }
            });

            result.get("property_value");

        } catch (Exception e) {
            logger.error("Error while creating Categories", e);
            throw new ServiceRuntimeException("Error while importing categories");
        }
    }

    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/private/archive/reindexdb",  method = RequestMethod.GET)
    public Iterable<SearchProduct> reIndexDB() {
        ProductCriteria criteria = new ProductCriteria();
        criteria.setStartPage(1);
        criteria.setPageSize(10);
        criteria.setMaxCount(10);
        try {
            MerchantStore store = merchantStoreService.getByCode("DEFAULT");
            ReadableProductList products = productFacade.getProductListsByCriterias(store, store.getDefaultLanguage(), criteria);
            List<SearchProduct> productsToIndex = products.getProducts().stream().map(p ->
                    SearchProduct.builder()
                            .id(p.getId())
                            .price(p.getPrice())
                            .merchant("DEFAULT")
                            .sku(p.getSku())
                            .title(p.getDescription().getTitle())
                            .availability(p.getQuantity())
                            .description(p.getDescription().getDescription())
                            .category(p.getCategories().stream().map(c -> c.getDescription().getName()).collect(Collectors.toList()))
                            .attributes( p.getProperties().stream().map(a -> Attribute.builder()
                                    .attributeId(a.getId())
                                    .name(a.getCode())
                                    .strValues(Stream.of(a.getPropertyValue().getName()).collect(Collectors.toSet()))
                                    .build()).collect(Collectors.toList()))
                            .build()
            ).collect(Collectors.toList());
            RestTemplate searchService = new RestTemplate();
            searchService.postForLocation(searchApiUrl + "/products", productsToIndex);// , new ParameterizedTypeReference<List<SearchProduct>>() {} );
            ResponseEntity<Page<SearchProduct>> result =  searchService.exchange(searchApiUrl + "/products", HttpMethod.GET, null,  new ParameterizedTypeReference<Page<SearchProduct>>() {});
            return result.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
