package com.salesmanager.shop.model.order;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class ReadableOrderProduct extends OrderProductEntity implements
		Serializable {

	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = 1L;
	private String productName;
	private String price;
	private String subTotal;
	private List<ReadableOrderProductAttribute> attributes = null;
	private List<ReadableOrderProductHistory> history;
	private String sku;
	private String image;

}
