package jp.co.kawakyo.nextengineapi.Entity;

import lombok.Data;

@Data
public class ItemInfo {
    /** 商品コード */
    private String itemCode;
    /** 商品名 */
    private String itemName;
    /** 商品オプション */
    private String itemOption;
    /** 商品価格(税抜) */
    private String itemPrice;
    /** 商品数量 */
    private String itemCount;
    /** 商品消費税率 */
    private String itemTaxrate;
}
