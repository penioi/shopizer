package com.salesmanager.shop.model.order;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class ReadableOrderProductHistory {

    private Date dateCreated;
    private String user;
    private Integer productQuantity;
    private BigDecimal productPrice;

}
