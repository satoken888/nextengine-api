package jp.co.kawakyo.nextengineapi.Entity;

import lombok.Data;

@Data
public class CustomerInfoForReceiveOrderForm {
    // お客様名
    String name;
    // お客様名カナ
    String kana;
    // 郵便番号
    String zip_code;
    // 住所１
    String address1;
    // 住所２
    String address2;
    // 電話番号
    String tel;
    // FAX
    String fax;
    // メールアドレス
    String mail_address;
}
