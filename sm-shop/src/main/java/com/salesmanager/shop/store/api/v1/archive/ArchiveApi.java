package com.salesmanager.shop.store.api.v1.archive;

import com.salesmanager.core.business.repositories.catalog.product.attribute.ProductOptionRepository;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.PersistableCategory;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import com.salesmanager.shop.model.entity.Entity;
import com.salesmanager.shop.store.api.exception.ServiceRuntimeException;
import com.salesmanager.shop.store.api.exception.UnauthorizedException;
import com.salesmanager.shop.store.controller.category.facade.CategoryFacade;
import com.salesmanager.shop.store.controller.manufacturer.facade.ManufacturerFacade;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionFacade;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionSetFacade;
import com.salesmanager.shop.store.controller.user.facade.UserFacade;
import com.shopizer.archive.CategoriesParser;
import com.shopizer.archive.XLSArchiver;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.checkerframework.checker.nullness.Opt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.annotations.ApiIgnore;

import javax.inject.Inject;
import java.io.IOException;
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
    private ProductOptionRepository productOptionRepository;

    @Inject
    private UserFacade userFacade;

    @Inject
    private ManufacturerService manufacturerService;

    @Inject
    private ManufacturerFacade manufacturerFacade;
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
}
