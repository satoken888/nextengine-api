package jp.co.kawakyo.nextengineapi.Entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderCheckListDetailsEntity {
    private String breakdownName;
    private String itemCode;
    private String itemName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer subTotal;

}
