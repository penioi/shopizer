package com.salesmanager.core.model.order.orderproduct;

import com.salesmanager.core.model.order.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.persistence.PostLoad;
import javax.persistence.PreUpdate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class OrderProductEntityListener {

    @PostLoad
    public void postLoad(OrderProduct orderProduct) {
        OrderProductHistory history = OrderProductHistory.builder()
                .priceOld(orderProduct.getPrices().iterator().next().getProductPrice())
                .productQuantityOld(orderProduct.getProductQuantity())
                .orderProduct(orderProduct).build();
        orderProduct.setOriginalState(history);
    }


    @PreUpdate
    public void preUpdate(OrderProduct orderProduct) {
        OrderProductHistory productHistory = orderProduct.getOriginalState();
        if(productHistory == null) return;
        productHistory.setDateCreated(new Date());
        productHistory.setPrice(orderProduct.getPrices().iterator().next().getProductPrice());
        productHistory.setProductQuantity(orderProduct.getProductQuantity());
        productHistory.setUser(SecurityContextHolder.getContext().getAuthentication().getName());

        if(productHistory.hasChange()) {
            List<OrderProductHistory> histories = new ArrayList<>(orderProduct.getHistory());
            histories.add(productHistory);
            orderProduct.setHistory(histories);
        }
    }
}
