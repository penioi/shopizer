package com.shopizer.archive;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.business.services.catalog.product.ProductService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductAttributeService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductOptionService;
import com.salesmanager.core.business.services.catalog.product.attribute.ProductOptionValueService;
import com.salesmanager.core.business.services.catalog.product.image.ProductImageService;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.attribute.ProductAttribute;
import com.salesmanager.core.model.catalog.product.attribute.ProductOption;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValue;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionValueDescription;
import com.salesmanager.core.model.catalog.product.availability.ProductAvailability;
import com.salesmanager.core.model.catalog.product.description.ProductDescription;
import com.salesmanager.core.model.catalog.product.image.ProductImage;
import com.salesmanager.core.model.catalog.product.image.ProductImageDescription;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.price.ProductPrice;
import com.salesmanager.core.model.catalog.product.type.ProductType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.model.catalog.product.product.PersistableProduct;
import com.salesmanager.shop.store.controller.product.facade.ProductOptionSetFacade;
import com.shopizer.archive.SheetRecord.RecordCell;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductsParser implements CatalogParser<PersistableProduct> {

    @Inject
    private Import anImport;
    @Autowired
    private ProductService productService;
    @Autowired
    private ManufacturerService manufacturerService;
    @Autowired
    private CategoryService categoryStoreService;
    @Autowired
    private ProductOptionSetFacade optionSetFacade;
    @Autowired
    private ProductTypeService productTypeService;
    @Autowired
    private ProductOptionService productOptionService;
    @Autowired
    private ProductOptionValueService productOptionValueService;
    @Autowired
    private ProductAttributeService productAttributeService;
    @Autowired
    private ProductImageService productImageService;

    public String getEntityName() {
        return "product";
    }
    /*
     * field_code field_name_en field_description_en price friendly_url metaTitle
     * metaDescription
     *
     * feature_availability_range feature_category feature_certifications
     * feature_collections feature_colour feature_composition feature_construction
     * feature_drape feature_kk_id feature_maeba_classif
     * feature_feature_main_material feature_materialist_society
     * feature_metres_available feature_new feature_old_price feature_on_homepage
     * feature_origin feature_other_features feature_pattern
     * feature_perceived_weight
     *
     * feature_price_range feature_resistance_to_crease feature_seasonality
     * feature_shipping_from feature_description feature_softness feature_stretch
     * feature_supplier_code feature_supplier_name
     * feature_sustainability_credentials feature_swatches_available
     * feature_type_of_fabric feature_unit feature_unit_description feature_warp
     * feature_weft feature_weight_in_g feature_width product_img_gallery
     *
     */

    public List<PersistableProduct> parse(MerchantStore store, Map<Integer, SheetRecord> data) {

        List<SheetRecord> combinedData = anImport.parseExpandedValuesUsing("field_code", data);

        List<Language> languages = store.getLanguages();
        Map<String, PersistableProduct> products = new TreeMap<>();

        Category category;
        ProductType productType = null;
        try {
            productType = productTypeService.getByCode("FABRIC", store, null);
            category = categoryStoreService.getByCode(store, "dead_stock_fabrics");
        } catch (ServiceException e) {
            e.printStackTrace();
            return null;
        }
        try {
            for (SheetRecord sheetRecord : combinedData) {

                try {
                    String code = sheetRecord.get("field_code");
                    log.info("Create PersistableProduct  {} ", code);
                    Product currentProduct = Optional.ofNullable(productService.getBySku(code, store))
                            .orElse(new Product());
                    currentProduct.setSku(code);
                    for (Language l : languages) {
                        ProductDescription pd = currentProduct.getDescriptions().stream()
                                .filter(d -> d.getLanguage().equals(l)).findFirst().orElse(new ProductDescription());
                        String name = sheetRecord.get("field_name_" + l.getCode());
                        pd.setTitle(name);
                        pd.setName(name);
                        pd.setDescription(sheetRecord.get("field_description_" + l.getCode()));
                        pd.setMetatagTitle(sheetRecord.get("metaTilte"));
                        pd.setMetatagDescription(sheetRecord.get("metaDescription"));
                        pd.setSeUrl(sheetRecord.get("friendly_url"));
                        pd.setLanguage(l);
                        pd.setProduct(currentProduct);
                        currentProduct.getDescriptions().add(pd);
                    }
                    // price

                    ProductAvailability availability = currentProduct.getAvailabilities().stream().findFirst()
                            .orElse(new ProductAvailability());
                    availability.setProductQuantity(sheetRecord.getInteger("feature_metres_available"));
                    ProductPrice productPrice = availability.getPrices().stream().findFirst()
                            .orElse(new ProductPrice());
                    productPrice.setDefaultPrice(true);
                    productPrice.setProductPriceAmount(new BigDecimal(sheetRecord.get("price")));
                    availability.getPrices().add(productPrice);
                    productPrice.setProductAvailability(availability);
                    availability.setProduct(currentProduct);
                    availability.setRegion(Constants.ALL_REGIONS);
                    availability.setMerchantStore(store);
                    currentProduct.getAvailabilities().add(availability);
                    /// manufacturer
                    Manufacturer manufacturer = Optional.ofNullable(sheetRecord.get("manufacturer"))
                            .map(manufacturerCode -> manufacturerService.getByCode(store, code))
                            .orElseGet(() -> manufacturerService.getByCode(store, "DEFAULT"));
                    currentProduct.setManufacturer(manufacturer);
                    currentProduct.getCategories().add(category);
                    currentProduct.setMerchantStore(store);
                    // product type
                    currentProduct.setType(productType);

                    try {
                        productService.save(currentProduct);
                    } catch (ServiceException e) {
                        e.printStackTrace();
                    }

                    // Process all feature_
                    List<RecordCell> feautreCells = sheetRecord.stream().filter(c -> c.getName().startsWith("feature_"))
                            .collect(Collectors.toList());
                    List<ProductAttribute> productProperites = new ArrayList<>();
                    for (RecordCell cell : feautreCells) {
                        ProductOption po = productOptionService.getByCode(store,
                                cell.getName().replace("feature_", ""));
                        if (po == null) {
                            System.out.println("WARN - No Product Option " + cell.getName().replace("feature_", ""));
                            continue;
                        }

                        for(String value : cell.getValues()) {
                            if (StringUtils.isNotEmpty(value)) {
                                String valueCode = value.startsWith(po.getCode()) ? value : po.getCode() + "_" + value;
                                valueCode = valueCode.replaceAll("[^\\w\\d]", "_");

                                ProductOptionValue optionValue = Optional.ofNullable(productOptionValueService.getByCode(store, valueCode)).orElse(new ProductOptionValue());
                                if (optionValue.getId() == null) {
                                    optionValue.setMerchantStore(store);
                                    optionValue.setProductOptionDisplayOnly(true);
                                    optionValue.setCode(valueCode);
                                    ProductOptionValueDescription description = new ProductOptionValueDescription();
                                    description.setLanguage(languages.get(0));
                                    description.setProductOptionValue(optionValue);
                                    description.setTitle(value);
                                    description.setName(value);
                                    optionValue.getDescriptions().add(description);
                                    productOptionValueService.saveOrUpdate(optionValue);
                                }
                                ProductAttribute attribute = currentProduct.getAttributes().stream()
                                        .filter(attr -> attr.getProductOption() != null
                                                && attr.getProductOption().getCode().equals(po.getCode())
                                                && attr.getProductOptionValue().getCode().equals(optionValue.getCode()))
                                        .findFirst().orElse(new ProductAttribute());
                                attribute.setProductOption(po);
                                attribute.setAttributeDisplayOnly(true);
                                attribute.setProductOptionValue(optionValue);
                                attribute.setProduct(currentProduct);
                                productProperites.add(attribute);
                            }
                        }

                    }

                    currentProduct.getAttributes().clear();
                    currentProduct.getAttributes().addAll(productProperites);

                    try {
                        productService.save(currentProduct);
                    } catch (ServiceException e) {
                        e.printStackTrace();
                    }

                    RecordCell imgFilenames = sheetRecord.getCell("product_img_gallery_file");
                    RecordCell imgDescriptions = sheetRecord.getCell("product_img_gallery_description_en");
                    if (imgFilenames != null) {
                        String imgFname;
                        while ((imgFname = imgFilenames.pop()) != null) {
                            final String fileName = imgFname;
                            ProductImage productImage = currentProduct.getImages().stream().filter(pi -> pi.getProductImage().equals(fileName)).findFirst().orElse(new ProductImage());
                            productImage.setProduct(currentProduct);
                            productImage.setProductImageUrl(imgFname);
                            productImage.setProductImage(imgFname);
                            String description = imgDescriptions.hasValue() ? imgDescriptions.pop() : null;
                            if (description != null) {
                                ProductImageDescription piDescription = productImage.getDescriptions().stream().findFirst().orElse(new ProductImageDescription());
                                piDescription.setDescription(description);
                                piDescription.setName(description);
                                piDescription.setAltTag(description);
                                piDescription.setLanguage(languages.get(0));
                                productImage.getDescriptions().add(piDescription);
                                piDescription.setProductImage(productImage);
                            }
                            productImageService.save(productImage);
                        }
                    }

                } catch (Exception ex) {
                    log.error("ERROR INSERTING PRODUCT {} " + sheetRecord, ex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(products.values());
    }

}
