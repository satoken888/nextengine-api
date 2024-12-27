package jp.co.kawakyo.nextengineapi.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import jp.co.kawakyo.nextengineapi.Entity.OrderCheckListDetailsEntity;
import jp.co.kawakyo.nextengineapi.Entity.OrderCheckListEntity;
import jp.co.kawakyo.nextengineapi.utils.Constant;
import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;

@Controller
public class DmCheckController {

    @GetMapping("/dmcheck")
    private String viewCheckPage(Model model) {
        model.addAttribute("message", "ここにメッセージが表示されます。");
        return "check";
    }

    @PostMapping("/dmcheck")
    private String checkCSVFile(Model model, @RequestParam("checkListFile") MultipartFile csvFile) {

        // キー情報の取得
        Map<String, Integer> keyMap = convertKeyMap(csvFile);

        // 受注情報データオブジェクトを生成
        List<OrderCheckListEntity> orderCheckList = getOrderCheckList(csvFile, keyMap);

        // チェック作業の実施
        // 実施結果はメッセージとして受注番号を出力し、
        // ユーザーに知らせるようにする。
        String alertMessage = execCheckOrder(orderCheckList);

        model.addAttribute("message", alertMessage);
        return "check";
    }

    private String execCheckOrder(List<OrderCheckListEntity> orderCheckList) {
        String alertMessage = "";

        // 後払い手数料のチェック処理
        alertMessage += checkDeferredFee(orderCheckList);

        // ６４８０円以上１０８００円未満の購入の場合に商品コード250110が入っているか確認
        alertMessage += checkItemCodeAndPostage(orderCheckList, 6480, "250110", null,
                Arrays.asList("837"));

        // カムバックのプレゼントチェック
        alertMessage += checkPresentComeback(orderCheckList, Constant.COMEBACK_ITEMCODE_LIST, "241229");

        // カムバックの送料チェック
        alertMessage += checkPostageComeback(orderCheckList, Constant.COMEBACK_ITEMCODE_LIST);

        // カムバックのポイント設定抜けチェック
        alertMessage += checkPointSetting(orderCheckList, Constant.COMEBACK_ITEMCODE_LIST);

        // 新規ハガキのプレゼント抜けチェック
        alertMessage += checkNewPostcardPresent(orderCheckList);

        // 先様のお名前納品書に記載無しチェック
        alertMessage += checkGiftNoName(orderCheckList);

        // 送料込商品ある場合の送料ヌケモレチェック
        alertMessage += checkShippingIncludedItem(orderCheckList);

        // 改行コードをHTML用に変換する
        alertMessage = convertLineBreakCode(alertMessage);

        return alertMessage;
    }

    private String checkGiftNoName(List<OrderCheckListEntity> orderCheckList) {
        String alertMessage = "【先様のお名前が受注メモに入ってない】\n";

        Set<String> telSet = new HashSet<String>();
        for (OrderCheckListEntity entity : orderCheckList) {
            if (!StringUtils.equals(entity.getBuyerTel(), entity.getDestTel())) {
                // 依頼主と送り先が違う（先様）の受注の場合
                if (!StringUtils.contains(entity.getOrderMemo(), "様分")) {
                    // 先様注文の場合に、「様分」の表記が含まれない場合は
                    // エラー表示する。
                    alertMessage += entity.getOrderNo() + "\n";
                }

                // 先様へ発送する方の依頼主電話番号をリスト化
                telSet.add(entity.getBuyerTel());
            }
        }
        alertMessage += "\n【先様注文者のご自宅分受注メモにご自宅分記載なし】\n";

        if (telSet.size() > 0) {
            for (OrderCheckListEntity entity : orderCheckList) {
                if (telSet.contains(entity.getBuyerTel())
                        && StringUtils.equals(entity.getBuyerTel(), entity.getDestTel())) {
                    // 先様の注文をしたお客様のご自宅分注文の場合
                    // 受注メモにご自宅分の記載がなければエラー表示する。
                    if (!StringUtils.contains(entity.getOrderMemo(), "ご依頼主様分")) {
                        alertMessage += entity.getOrderNo() + "\n";
                    }
                }
            }
        }
        alertMessage += "\n";

        return alertMessage;
    }

    private String checkNewPostcardPresent(List<OrderCheckListEntity> orderCheckList) {
        String alertMessage = "【新規ハガキのプレゼント抜け】\n";

        for (OrderCheckListEntity entity : orderCheckList) {
            boolean isError = false;
            boolean isPresentTarget = false;
            List<OrderCheckListDetailsEntity> detailsList = entity.getDetailsList();
            for (OrderCheckListDetailsEntity detail : detailsList) {
                // 受注リストをまわして、プレゼント対象かのチェックを行う
                // 550-25を２個以上購入しているか
                // 550-22,550-23,550-24を１個以上購入しているかが条件
                if ((StringUtils.equals("550-25", detail.getItemCode()) && detail.getQuantity() > 1)
                        || StringUtils.equals("550-22", detail.getItemCode())
                        || StringUtils.equals("550-23", detail.getItemCode())
                        || StringUtils.equals("550-24", detail.getItemCode())) {
                    isPresentTarget = true;
                    break;
                }
            }

            if (isPresentTarget) {
                // プレゼント対象と判断した場合

                isError = true;
                for (OrderCheckListDetailsEntity detail : detailsList) {
                    // 9000-94のプレゼントが付与されているか確認する。
                    if (StringUtils.equals("9000-94", detail.getItemCode())) {
                        // 注文内容の中に9000-94があれば問題なしとする。
                        isError = false;
                        break;
                    }
                }
            }

            if (isError) {
                alertMessage += entity.getOrderNo() + "\n";
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    private String convertLineBreakCode(String alertMessage) {
        return alertMessage.replace("\n", "<br />");
    }

    private String checkShippingIncludedItem(List<OrderCheckListEntity> orderCheckList) {
        String alertMessage = "【送料込商品ある場合の送料削除抜け】\n";

        for (OrderCheckListEntity entity : orderCheckList) {
            boolean existShippingIncludedItem = false;
            for (OrderCheckListDetailsEntity detail : entity.getDetailsList()) {
                if (StringUtils.contains(detail.getItemName(), "送料")) {
                    existShippingIncludedItem = true;
                    break;
                }
            }
            if (existShippingIncludedItem) {
                // 送料込み商品を購入している場合
                boolean isError = true;
                for (OrderCheckListDetailsEntity detail : entity.getDetailsList()) {
                    // if (StringUtils.equals(detail.getBreakdownName(), "運賃") &&
                    // detail.getSubTotal() == 0) {
                    // isError = false;
                    // break;
                    // }
                    if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() == 0
                            && !StringUtils.equals("沖縄県", entity.getDestPrefecture())) {
                        // 受注内容が沖縄県以外の送り先で、なおかつ送料無料になっている場合
                        isError = false;
                        break;
                    } else if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() == 2020
                            && StringUtils.equals("沖縄県", entity.getDestPrefecture())) {
                        // 受注内容が沖縄県への送り先で、なおかつ送料が追加送料の2020円が追加されている場合
                        isError = false;
                        break;

                    }
                }
                if (isError) {
                    alertMessage += entity.getOrderNo() + "\n";
                }
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    private String checkPointSetting(List<OrderCheckListEntity> orderCheckList, List<String> comebackItemcodeList) {
        String alertMessage = "【カムバックのポイント設定抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            // カムバックの注文の判別を行う
            boolean isComeback = false;
            for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                if (comebackItemcodeList.contains(detail.getItemCode())) {
                    isComeback = true;
                    break;
                }
            }

            // カムバックの受注で受注区分が7でない場合はNGとする
            if (isComeback && !StringUtils.equals(order.getDivOrder(), "7")) {
                alertMessage += order.getOrderNo() + "\n";
            }
        }

        return alertMessage;
    }

    private String checkPostageComeback(List<OrderCheckListEntity> orderCheckList, List<String> comebackItemcodeList) {
        String alertMessage = "【カムバックの送料抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            // カムバックの注文の判別を行う
            boolean isComeback = false;
            for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                if (comebackItemcodeList.contains(detail.getItemCode())) {
                    isComeback = true;
                    break;
                }
            }

            // カムバックの注文と判定された場合、送料削除漏れチェックを実施する。
            if (isComeback) {
                boolean isError = false;
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() != 0
                            && !StringUtils.equals("沖縄県", order.getDestPrefecture())) {
                        // 受注内容が沖縄県への送り先ではなく、なおかつ送料無料になっていない場合
                        isError = true;
                        break;
                    } else if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() != 2020
                            && StringUtils.equals("沖縄県", order.getDestPrefecture())) {
                        // 受注う内容が沖縄県への送り先で、なおかつ送料が追加送料の2020円が追加されていない場合
                        isError = true;
                        break;

                    }
                }
                if (isError) {
                    alertMessage += order.getOrderNo() + "\n";
                }
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    private String checkPresentComeback(List<OrderCheckListEntity> orderCheckList, List<String> comebackItemcodeList,
            String presentItemCode) {
        String alertMessage = "【カムバックのプレゼント抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            // カムバックの注文の判別を行う
            boolean isComeback = false;

            for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                // 該当商品を注文していればカムバックの受注とする
                if (comebackItemcodeList.contains(detail.getItemCode())) {
                    isComeback = true;
                    break;
                }
            }

            // カムバックの注文と判定された場合、プレゼント抜けチェックを実施する。
            if (isComeback) {
                boolean isError = true;
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (StringUtils.equals(detail.getItemCode(), presentItemCode)) {
                        isError = false;
                    }
                }
                if (isError) {
                    alertMessage += order.getOrderNo() + "\n";
                }
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    /**
     * しきい値超えの場合の受注で対象商品が抜けているかのチェック
     * 
     * @param thresholdPrice
     * @param string
     * @return
     */
    private String checkItemCodeAndPostage(List<OrderCheckListEntity> orderCheckList, int thresholdPrice,
            String presentItemCode,
            List<String> excludeEventCodeList, List<String> targetEventCodeList) {

        String alertMessage = "【" + thresholdPrice + "円以上購入の際のコード(" + presentItemCode + ")抜け、送料無料対応抜け】\n";

        for (OrderCheckListEntity entity : orderCheckList) {

            boolean existShippingIncludedItem = entity.getDetailsList().stream()
                    .filter(l -> StringUtils.contains(l.getItemName(), "送料")).count() > 0 ? true : false;

            if (entity.getTotalEarnings() >= thresholdPrice && entity.getTotalEarnings() <= 10800
                    && (excludeEventCodeList == null || !excludeEventCodeList.contains(entity.getEventCode()))
                    && (targetEventCodeList == null || targetEventCodeList.contains(entity.getEventCode()))
                    && !existShippingIncludedItem) {
                // 該当料金以上の購入かつ、対象外イベントコードではないかつ、対象イベントコードかつ、送料込商品を含んでいない場合

                boolean isItemCodeNG = true;
                boolean isPostageNG = true;
                for (OrderCheckListDetailsEntity detail : entity.getDetailsList()) {

                    if (StringUtils.equals(detail.getItemCode(), presentItemCode)
                            || StringUtils.equals(detail.getItemCode(), "7363")
                            || StringUtils.equals(detail.getItemCode(), "7364")) {
                        // プレゼントのコードが含まれているまたは、福麺箱・具付福麺箱を購入の場合はプレゼントコード不要とする
                        isItemCodeNG = false;
                    }

                    if (StringUtils.equals("運賃", detail.getBreakdownName()) && detail.getSubTotal() == 0) {
                        isPostageNG = false;
                    }
                }

                if (isItemCodeNG || isPostageNG) {
                    alertMessage += entity.getOrderNo() + "\n";
                }
            } else if (entity.getTotalEarnings() < thresholdPrice
                    && (excludeEventCodeList == null || !excludeEventCodeList.contains(entity.getEventCode()))
                    && (targetEventCodeList == null || targetEventCodeList.contains(entity.getEventCode()))
                    && !existShippingIncludedItem) {
                // 該当金額未満の場合は送料が付与されているかチェックする。
                boolean isError = false;
                for (OrderCheckListDetailsEntity detail : entity.getDetailsList()) {
                    if (StringUtils.equals("運賃", detail.getBreakdownName()) && detail.getSubTotal() == 0) {
                        isError = true;
                        break;
                    }
                }

                if (isError) {
                    alertMessage += entity.getOrderNo() + "\n";
                }
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    private String checkDeferredFee(List<OrderCheckListEntity> orderCheckList) {
        String alertMessage = "【後払い手数料抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            if (StringUtils.equals(order.getPaymentMethod(), "クロネコ後払い")) {
                boolean isError = true;
                if (checkSameCustomerSameDay(order.getBuyerCode(), orderCheckList)) {
                    // 同日出荷で同じ購入者の方がいる場合
                    // 同日出荷の中で手数料の記載がある受注が１件のみであればisErrorはfalseにする。
                    int existDeferredFee = 0;
                    for (OrderCheckListEntity entity : orderCheckList) {
                        if (StringUtils.equals(order.getBuyerCode(), entity.getBuyerCode())) {
                            List<OrderCheckListDetailsEntity> details = entity.getDetailsList();
                            for (OrderCheckListDetailsEntity detail : details) {
                                if (StringUtils.equals("手数料1", detail.getBreakdownName())) {
                                    existDeferredFee++;
                                    break;
                                }
                            }
                        }
                    }
                    if (existDeferredFee == 1) {
                        isError = false;
                    }
                } else {
                    // 同日出荷で同じ購入者の方がいない場合
                    for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                        if (StringUtils.equals(detail.getBreakdownName(), "手数料1")) {
                            isError = false;
                        }
                    }
                }

                if (isError) {
                    alertMessage += order.getOrderNo() + "\n";
                }
            }
        }

        alertMessage += "\n";

        return alertMessage;
    }

    /**
     * 同日出荷で同じ購入者の方がいるかのチェック
     *
     * @param buyerCode
     * @param orderCheckList
     * @return
     */
    private boolean checkSameCustomerSameDay(String buyerCode, List<OrderCheckListEntity> orderCheckList) {
        boolean exist = false;

        int sameCount = 0;
        for (OrderCheckListEntity entity : orderCheckList) {
            if (StringUtils.equals(entity.getBuyerCode(), buyerCode)) {
                sameCount++;
            }
            if (sameCount > 1) {
                exist = true;
                break;
            }
        }
        return exist;
    }

    private Map<String, Integer> convertKeyMap(MultipartFile csvFile) {
        HashMap<String, Integer> rtnMap = new HashMap<String, Integer>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), "SJIS"))) {
            String line = br.readLine();
            line = ConvertUtils.trimDoubleQuot(line);
            line = ConvertUtils.trimDoubleQuot(line);
            String[] split = line.split("\",\"");

            for (int i = 0; i < split.length; i++) {
                rtnMap.put(split[i], i);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rtnMap;

    }

    /**
     * csvファイルから受注情報のエンティティを作成する。
     * エンティティには明細リストを持つようにし、そこで商品内容を閲覧できるようにする。
     * 
     * @param csvFile
     * @param keyMap
     * @return
     */
    private List<OrderCheckListEntity> getOrderCheckList(MultipartFile csvFile, Map<String, Integer> keyMap) {
        List<OrderCheckListEntity> rtnList = new ArrayList<OrderCheckListEntity>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), "SJIS"))) {

            String line;
            line = br.readLine();
            while ((line = br.readLine()) != null) {
                line = ConvertUtils.trimDoubleQuot(line);
                String[] split = line.split("\",\"");

                String orderNo = split[keyMap.get("受注番号")];
                String buyerCode = split[keyMap.get("依頼主ｺｰﾄﾞ")];
                String buyerTel = split[keyMap.get("依頼主TEL")];
                String destTel = split[keyMap.get("届け先TEL")];
                String eventNo = split[keyMap.get("ｲﾍﾞﾝﾄNO")];
                String eventName = split[keyMap.get("ｲﾍﾞﾝﾄ名")];
                Integer subtotal = Integer.valueOf(split[keyMap.get("小計請求金額")]);
                String destPrefecture = split[keyMap.get("届け先都道府県")];
                String orderMemo = split[keyMap.get("受注メモ欄")];
                String paymentMethod = split[keyMap.get("請求書種別名称")];
                String coolDiv = split[keyMap.get("クール区分")];
                String breakdownName = split[keyMap.get("内訳名称")];
                String itemCode = split[keyMap.get("商品ｺｰﾄﾞ")];
                String itemName = split[keyMap.get("商品名称")];
                Integer unitPrice = Integer.parseInt(split[keyMap.get("税込単価")]);
                Integer quantity = Integer.parseInt(split[keyMap.get("数量")]);
                Integer detailsSubTotal = Integer.parseInt(split[keyMap.get("税込金額")]);
                // 受注区分はファイル内に2箇所あるので、直接数字でindexを指定
                String divOrder = split[88];
                OrderCheckListDetailsEntity detailsEntity = new OrderCheckListDetailsEntity(breakdownName,
                        itemCode, itemName, unitPrice, quantity, detailsSubTotal);

                boolean isExist = false;
                for (OrderCheckListEntity entity : rtnList) {
                    if (entity != null && StringUtils.equals(entity.getOrderNo(), orderNo)) {
                        // 明細を追加
                        entity.getDetailsList().add(detailsEntity);
                        // 請求金額から手数料とポイント等を差し引く
                        if (StringUtils.equals(breakdownName, "運賃") || StringUtils.equals(breakdownName, "ｺﾚｸﾄ手数料")
                                || StringUtils.equals(breakdownName, "手数料1")
                                || StringUtils.equals(breakdownName, "ポイント利用")) {
                            entity.setTotalEarnings(entity.getTotalEarnings() - detailsSubTotal);
                        }
                        isExist = true;
                    }
                }
                if (!isExist) {
                    // エンティティを新規作成し、明細も追加
                    List<OrderCheckListDetailsEntity> detailsEntityList = new ArrayList<OrderCheckListDetailsEntity>();
                    detailsEntityList.add(detailsEntity);
                    // 請求金額から手数料とポイント等を差し引く
                    if (StringUtils.equals(breakdownName, "運賃") || StringUtils.equals(breakdownName, "ｺﾚｸﾄ手数料")
                            || StringUtils.equals(breakdownName, "手数料1")
                            || StringUtils.equals(breakdownName, "ポイント利用")) {
                        subtotal = subtotal - detailsSubTotal;
                    }
                    OrderCheckListEntity newEntity = new OrderCheckListEntity(orderNo, divOrder, buyerCode, buyerTel,
                            destTel,
                            subtotal,
                            destPrefecture, orderMemo, eventNo,
                            eventName,
                            paymentMethod, coolDiv,
                            detailsEntityList);
                    rtnList.add(newEntity);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return rtnList;
    }
}
