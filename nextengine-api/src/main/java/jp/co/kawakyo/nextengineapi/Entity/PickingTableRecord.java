package jp.co.kawakyo.nextengineapi.Entity;

import java.util.List;

import lombok.Data;

@Data
public class PickingTableRecord {
    String itemCode;
    String itemName;
    String total;
    List<String> sumList;
    String ecTotal;
    String dmTotal;

    public PickingTableRecord() {
        
    }

    public PickingTableRecord(String itemCode,String itemName,String total,String ecTotal,String dmTotal) {
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.total = total;
        this.ecTotal = ecTotal;
        this.dmTotal = dmTotal;
    }
}
