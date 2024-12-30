package com.salesmanager.shop.model.order.v1;

import java.io.Serial;
import java.util.List;

import com.salesmanager.core.model.shipping.ShippingOption;
import com.salesmanager.shop.model.customer.ReadableBilling;
import com.salesmanager.shop.model.customer.ReadableDelivery;
import com.salesmanager.shop.model.order.ReadableOrderProduct;
import com.salesmanager.shop.model.order.total.ReadableTotal;
import com.salesmanager.shop.model.order.transaction.ReadablePayment;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReadableOrder extends Order {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;

	private ReadableBilling billing;
	private ReadableDelivery delivery;
	private ShippingOption shippingOption;               
	private ReadablePayment payment;
	private ReadableTotal total;
	private List<ReadableOrderProduct> products;


}
