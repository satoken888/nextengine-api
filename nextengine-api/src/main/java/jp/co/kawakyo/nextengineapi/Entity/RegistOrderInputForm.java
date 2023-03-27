package jp.co.kawakyo.nextengineapi.Entity;

import lombok.Data;

@Data
public class RegistOrderInputForm {

    /** 店舗伝票番号 */
    String shopOrderNumber;

    /** 受注日 */
    String orderDate;

    /** 受注郵便番号 */
    String buyerZipcode;

    /** 受注住所１ */
    String buyerAddress1;

    /** 受注住所２ */
    String buyerAddress2;

    /** 受注名 */
    String buyerName;

    /** 受注名カナ */
    String buyerKana;

    /** 受注電話番号 */
    String buyerTel;

    /** 受注メールアドレス */
    // 画面上には入力項目なし
    String buyerMail;

    /** 発送郵便番号 */
    String destZipCode;

    /** 発送先住所１ */
    String destAddress1;

    /** 発送先住所２ */
    String destAddress2;

    /** 発送先名 */
    String destName;

    /** 発送先カナ */
    String destKana;

    /** 発送電話番号 */
    String destTel;

    /** 支払い方法 */
    String paymentMethod;

    /** 発送方法 */
    String shippingMethod;

    /** 商品計 */
    String itemAllPrice;

    /** 商品計（税込） */
    String itemAllPrice_taxInclude;

    /** 税金 */
    String taxPrice;

    /** 発送料 */
    String shippingPrice;

    /** 手数料 */
    String commisionPrice;

    /** ポイント */
    String usePoint;

    /** その他費用 */
    String otherPrice;

    /** 合計金額 */
    String billingPrice;

    /** ギフトフラグ */
    boolean giftFlag;

    /** 時間帯指定 */
    String shippingTimeZone;

    /** 日付指定 */
    String deliveryDate;

    /** 出荷予定日 */
    String shippingSchedule;

    /** 作業者欄 */
    String workerArea;

    /** 備考 */
    String remarks;

    /** 発送伝票備考欄 */
    String invoiceWrite;

    /** 商品名 */
    String itemName;

    /** 商品コード */
    String itemCode;

    /** 商品価格 */
    String itemPrice;

    /** 受注数量 */
    String itemCount;

    /** 商品オプション */
    String itemOption;

    /** 出荷済フラグ */
    // 画面には表示なし
    boolean shippedFlag;

    /** 顧客区分 */
    // 画面には表示なし
    String customerDiv;

    /** 顧客コード */
    // 画面には表示なし
    String customerCode;

    /** 消費税率（%） */
    String itemTaxRate;

    /** のし */
    // 画面には表示なし
    String noshiContent;

    /** ラッピング */
    // 画面には表示なし
    String wrappingContent;

    /** メッセージ */
    // 画面には表示なし
    String message;

    /** 温度帯 */
    String coolDiv;
}
