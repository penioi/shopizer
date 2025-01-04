package com.salesmanager.core.model.order.orderproduct;

import com.salesmanager.core.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data // Lombok
@EqualsAndHashCode
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="ORDER_PRODUCT_HISTORY" )
public class OrderProductHistory implements Serializable {

    @Id
    @Column(name="ORDER_PRODUCT_HISTORY_ID")
    @TableGenerator(name = "TABLE_GEN", table = "SM_SEQUENCER", pkColumnName = "SEQ_NAME", valueColumnName = "SEQ_COUNT", pkColumnValue = "ORDER_PRODUCT_HISTORY_ID_NEXT_VALUE")
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "TABLE_GEN")
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "DATE_CREATED")
    private Date dateCreated = new Date();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_PRODUCT_ID", nullable = false)
    private OrderProduct orderProduct;

    @Column(name = "PRODUCT_QUANTITY")
    private Integer productQuantity;

    @Column(name = "PRODUCT_QUANTITY_OLD")
    private Integer productQuantityOld;

    @Column(name = "PRODUCT_PRICE")
    private BigDecimal price;

    @Column(name = "PRODUCT_PRICE_OLD")
    private BigDecimal priceOld;

    @Column(name = "USER")
    private String user;

    public boolean hasChange() {
        return  (priceOld != null && price != null && !priceOld.equals(price))
                || productQuantityOld != null && productQuantity != null && !productQuantityOld.equals(productQuantity);
    }
}
