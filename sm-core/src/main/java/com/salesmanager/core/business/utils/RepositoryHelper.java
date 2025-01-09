package com.salesmanager.core.business.utils;

import javax.persistence.Query;

import com.salesmanager.core.model.common.Criteria;
import com.salesmanager.core.model.common.GenericEntityList;

/**
 * Helper for Spring Data JPA
 *
 * @author carlsamson
 */
public class RepositoryHelper {

    @SuppressWarnings("rawtypes")
    public static Query paginateQuery(Query q, Number count, GenericEntityList entityList, Criteria criteria) {

        if (entityList == null) {
            entityList = new GenericEntityList();
        }

        if (criteria.isLegacyPagination()) {
            if (criteria.getMaxCount() > 0) {
                q.setFirstResult(criteria.getStartIndex());
                q.setMaxResults(Math.min(criteria.getMaxCount(), count.intValue()));
            }
        } else {
            int firstResult = (criteria.getStartPage() - 1)  * criteria.getPageSize();
            q.setFirstResult(firstResult);
            q.setMaxResults(criteria.getPageSize());
            int lastPageNumber = (int) Math.ceil(count.doubleValue() / criteria.getPageSize());
            entityList.setTotalPages(lastPageNumber);
            entityList.setTotalCount(count.intValue());
        }

        return q;

    }

}
