package com.shopizer.archive;

import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.model.entity.Entity;

import java.util.List;
import java.util.Map;

public interface CatalogParser<T extends Entity> {

    String getEntityName();
    List<T> parse(MerchantStore store, Map<Integer, SheetRecord> data);
}
