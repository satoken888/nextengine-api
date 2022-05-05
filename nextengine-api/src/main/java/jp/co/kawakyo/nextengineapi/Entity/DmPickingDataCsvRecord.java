package jp.co.kawakyo.nextengineapi.Entity;

import lombok.Data;

@Data
public class DmPickingDataCsvRecord {

	private String itemCd;
	private String itemName;
	private Long itemQuantity;
	
	public DmPickingDataCsvRecord(String itemCd,String itemName,Long itemQuantity) {
		this.itemCd = itemCd;
		this.itemName = itemName;
		this.itemQuantity = itemQuantity;
	}
}
