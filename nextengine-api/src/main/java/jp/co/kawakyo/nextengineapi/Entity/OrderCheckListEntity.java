package jp.co.kawakyo.nextengineapi.Entity;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderCheckListEntity {

    /** 受注番号 */
    private String orderNo;
    /** 購入者コード */
    private String buyerCode;
    /** 購入者電話番号 */
    private String buyerTel;
    /** 送り先様電話番号 */
    private String destTel;

    /** 小計金額（送料・手数料・ポイント計算済み） */
    private Integer totalEarnings;

    /** 届け先都道府県 */
    private String destPrefecture;

    /** 受注メモ欄 */
    private String orderMemo;

    /** イベントコード */
    private String eventCode;
    /** イベント名 */
    private String eventName;

    /** 支払い方法 */
    private String paymentMethod;

    /** クール区分 */
    private String coolDiv;

    /** 明細行情報リスト */
    private List<OrderCheckListDetailsEntity> detailsList;
}
