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

    /** 小計金額（送料・手数料・ポイント計算済み） */
    private Integer totalEarnings;

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
