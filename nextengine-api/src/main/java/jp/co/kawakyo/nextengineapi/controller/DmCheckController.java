package jp.co.kawakyo.nextengineapi.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // ●●●円以上購入の際のプレゼント抜けチェック
        alertMessage += checkPresent(orderCheckList, 6480, "241010");

        // カムバックのプレゼントチェック
        alertMessage += checkPresentComeback(orderCheckList, Constant.COMEBACK_ITEMCODE_LIST);

        // カムバックの送料チェック
        alertMessage += checkPostageComeback(orderCheckList, Constant.COMEBACK_ITEMCODE_LIST);

        // 送料込商品ある場合の送料ヌケモレチェック
        alertMessage += checkShippingIncludedItem(orderCheckList);

        // 改行コードをHTML用に変換する
        alertMessage = convertLineBreakCode(alertMessage);

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
                    if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() == 0) {
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

    private String checkPostageComeback(List<OrderCheckListEntity> orderCheckList, List<String> comebackItemcodeList) {
        String alertMessage = "【カムバックの送料抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            // カムバックの注文の判別を行う
            boolean isComeback = false;
            if (StringUtils.equals("829", order.getEventCode()) || StringUtils.equals("830", order.getEventCode())) {
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (comebackItemcodeList.contains(detail.getItemCode())) {
                        isComeback = true;
                        break;
                    }
                }
            }
            // カムバックの注文と判定された場合、送料削除漏れチェックを実施する。
            if (isComeback) {
                boolean isError = false;
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (StringUtils.equals(detail.getBreakdownName(), "運賃") && detail.getSubTotal() != 0) {
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

    private String checkPresentComeback(List<OrderCheckListEntity> orderCheckList, List<String> comebackItemcodeList) {
        String alertMessage = "【カムバックのプレゼント抜け】\n";

        for (OrderCheckListEntity order : orderCheckList) {
            // カムバックの注文の判別を行う
            boolean isComeback = false;
            if (StringUtils.equals("829", order.getEventCode()) || StringUtils.equals("830", order.getEventCode())) {
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (comebackItemcodeList.contains(detail.getItemCode())) {
                        isComeback = true;
                        break;
                    }
                }
            }

            // カムバックの注文と判定された場合、プレゼント抜けチェックを実施する。
            if (isComeback) {
                boolean isError = true;
                for (OrderCheckListDetailsEntity detail : order.getDetailsList()) {
                    if (StringUtils.equals(detail.getItemCode(), "241031")) {
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
    private String checkPresent(List<OrderCheckListEntity> orderCheckList, int thresholdPrice, String presentItemCode) {

        String alertMessage = "【" + thresholdPrice + "円以上購入の際のプレゼント抜け】\n";

        for (OrderCheckListEntity entity : orderCheckList) {
            if (entity.getTotalEarnings() >= thresholdPrice && !StringUtils.equals("829", entity.getEventCode())
                    && !StringUtils.equals("830", entity.getEventCode())
                    && !StringUtils.equals(entity.getCoolDiv(), "冷凍")) {
                boolean isError = true;
                for (OrderCheckListDetailsEntity detail : entity.getDetailsList()) {
                    if (StringUtils.equals(detail.getItemCode(), presentItemCode)) {
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
                String eventNo = split[keyMap.get("ｲﾍﾞﾝﾄNO")];
                String eventName = split[keyMap.get("ｲﾍﾞﾝﾄ名")];
                Integer subtotal = Integer.valueOf(split[keyMap.get("小計請求金額")]);
                String paymentMethod = split[keyMap.get("請求書種別名称")];
                String coolDiv = split[keyMap.get("クール区分")];
                String breakdownName = split[keyMap.get("内訳名称")];
                String itemCode = split[keyMap.get("商品ｺｰﾄﾞ")];
                String itemName = split[keyMap.get("商品名称")];
                Integer unitPrice = Integer.parseInt(split[keyMap.get("税込単価")]);
                Integer quantity = Integer.parseInt(split[keyMap.get("数量")]);
                Integer detailsSubTotal = Integer.parseInt(split[keyMap.get("税込金額")]);
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
                    OrderCheckListEntity newEntity = new OrderCheckListEntity(orderNo, buyerCode, subtotal, eventNo,
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
