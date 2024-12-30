package com.salesmanager.shop.model.order.v1;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.salesmanager.shop.model.entity.Entity;
import com.salesmanager.shop.model.order.OrderAttribute;
import com.salesmanager.shop.model.order.OrderProduct;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class Order extends Entity {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	
	private boolean customerAgreement;
	private String comments;
	private String currency;
	private List<OrderAttribute> attributes = new ArrayList<OrderAttribute>();
}
