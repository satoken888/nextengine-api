package jp.co.kawakyo.nextengineapi.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.kintone.client.KintoneClient;
import com.kintone.client.KintoneClientBuilder;
import com.kintone.client.model.record.MultiLineTextFieldValue;
import com.kintone.client.model.record.NumberFieldValue;
import com.kintone.client.model.record.Record;
import com.kintone.client.model.record.SingleLineTextFieldValue;

@Component
public class KintoneConnect {

    public List<Record> getCustomerInfo(String tel) {
        List<Record> rtnRecords = new ArrayList<Record>();

        try (KintoneClient client = KintoneClientBuilder.create("https://kawakyo.cybozu.com")
                .authByApiToken("wjR03hmiYcN6bTagOa0fG4kcOxXilBd5mXnpnMJo").build()) {
            rtnRecords = client.record().getRecords(61, "tel = " + tel);

        } catch (IOException e) {
            e.printStackTrace();
        }

        return rtnRecords;
    }

    /**
     * kintoneへの顧客情報登録処理
     * 
     * @param name
     * @param kana
     * @param tel
     * @param zipcode
     * @param address1
     * @param address2
     * @param memo
     * @param usablePoint
     */
    public void registCustomerInfo(String name, String kana, String tel, String zipcode, String address1,
            String address2, String memo, String usablePoint) {

        try (KintoneClient client = KintoneClientBuilder.create("https://kawakyo.cybozu.com")
                .authByApiToken("wjR03hmiYcN6bTagOa0fG4kcOxXilBd5mXnpnMJo").build()) {
            Record registRecord = new Record();
            registRecord.putField("name", new SingleLineTextFieldValue(name));
            registRecord.putField("kana", new SingleLineTextFieldValue(kana));
            registRecord.putField("zipcode", new SingleLineTextFieldValue(zipcode));
            registRecord.putField("tel", new SingleLineTextFieldValue(tel));
            registRecord.putField("address1", new SingleLineTextFieldValue(address1));
            registRecord.putField("address2", new SingleLineTextFieldValue(address2));
            registRecord.putField("memo", new MultiLineTextFieldValue(memo));
            registRecord.putField("point",
                    new NumberFieldValue(usablePoint == null ? 0L : Long.valueOf(usablePoint)));
            client.record().addRecord(61L, registRecord);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * kintoneへの顧客情報更新処理
     * 
     * @param name
     * @param kana
     * @param tel
     * @param zipcode
     * @param address1
     * @param address2
     * @param memo
     * @param usablePoint
     */
    public void updateCustomerInfo(String id, String name, String kana, String tel, String zipcode, String address1,
            String address2, String memo, String usablePoint) {
        try (KintoneClient client = KintoneClientBuilder.create("https://kawakyo.cybozu.com")
                .authByApiToken("wjR03hmiYcN6bTagOa0fG4kcOxXilBd5mXnpnMJo").build()) {
            Record updateRecord = new Record();
            updateRecord.putField("name", new SingleLineTextFieldValue(name));
            updateRecord.putField("kana", new SingleLineTextFieldValue(kana));
            updateRecord.putField("zipcode", new SingleLineTextFieldValue(zipcode));
            updateRecord.putField("tel", new SingleLineTextFieldValue(tel));
            updateRecord.putField("address1", new SingleLineTextFieldValue(address1));
            updateRecord.putField("address2", new SingleLineTextFieldValue(address2));
            if (memo != null) {
                updateRecord.putField("memo", new MultiLineTextFieldValue(memo));
            }
            if (usablePoint != null) {
                updateRecord.putField("point",
                        new NumberFieldValue(Long.valueOf(usablePoint)));
            }
            client.record().updateRecord(61L, Long.valueOf(id), updateRecord);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
