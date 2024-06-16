package jp.co.kawakyo.nextengineapi.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kawakyo.nextengineapi.Entity.AnalyticsInputForm;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.base.NeToken;
import jp.co.kawakyo.nextengineapi.utils.Constant;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import lombok.Data;

@Controller
public class AnalyticsController extends BaseController {

    @GetMapping(value = "/analytics")
    private String showAnalyticsIndex(HttpServletRequest _request, HttpServletResponse _response, Model model) {

        logger.info("analytics start");

        // アクセストークンを取得
        if (getCurrentToken(_request) == null) {
            HashMap<String, Object> userInfo = neLogin(_request, _response, authClientProperty.getRedirectUrl());
            if (userInfo != null) {
                saveTokenToSession(_request, new NeToken(), userInfo);
            }
        }

        AnalyticsInputForm inputForm = new AnalyticsInputForm();
        inputForm.setAnalyticsDiv("itemRank");
        model.addAttribute("analyticsInputForm", inputForm);
        logger.info("analytics end");
        return "analytics";
    }

    @PostMapping(value = "/analytics")
    private String showItemRanking(HttpServletRequest _request, HttpServletResponse _response, Model model,
            @ModelAttribute AnalyticsInputForm analyticsInputForm) {

        String displayMessage = "";
        // 出荷予定日（開始日）を取得
        String inputStartPickingDate = analyticsInputForm.getInputStartPickingDate();
        // 出荷予定日（終了日）を取得
        String inputEndPickingDate = analyticsInputForm.getInputEndPickingDate();
        // 分析対象を取得
        String analyticsDiv = analyticsInputForm.getAnalyticsDiv();

        List<ItemInfo> itemRankingList = new ArrayList<ItemInfo>();

        // 分析対象が商品ランキングの場合
        if (StringUtils.equals("itemRank", analyticsDiv)) {

            // 入力チェック
            // 出荷予定日片方でも入力されてなかったらエラーとする
            if (StringUtils.isEmpty(inputStartPickingDate) || StringUtils.isEmpty(inputEndPickingDate)) {
                displayMessage = "出荷予定日を両方入力してください。";
            } else {

                // 両方入力されていた場合

                // 出荷予定日から有効な受注伝票番号のリストを取得する
                Set<String> orderNumberList = getOrderNumberList(_request, inputStartPickingDate, inputEndPickingDate);

                // 受注伝票番号のリストから対象期間の商品ランキングのリストを取得する
                itemRankingList = getItemRankingList(_request, orderNumberList);
                if (CollectionUtils.isEmpty(itemRankingList)) {
                    displayMessage = "検索結果が0件です。";
                }
            }
        }
        // 画面に商品ランキングリストを返す
        model.addAttribute("itemRankingList", itemRankingList);
        // 画面表示メッセージを返す
        model.addAttribute("displayMessage", displayMessage);
        // 画面表示を維持するために入力内容を引き継ぐ
        model.addAttribute("analyticsInputForm", analyticsInputForm);
        return "analytics";
    }

    /**
     * 受注番号リスト取得
     * 
     * @param _request
     * @param inputStartPickingDate
     * @param inputEndPickingDate
     * @return
     */
    private Set<String> getOrderNumberList(HttpServletRequest _request, String inputStartPickingDate,
            String inputEndPickingDate) {
        HashSet<String> orderNumberSet = new HashSet<String>();

        // 受注情報APIを呼び出し、出荷予定日に適する受注情報を取得する
        HashMap<String, Object> orderInfoResponse = neApiExecute(getCurrentToken(_request),
                NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH,
                createReceiveOrderApiParam(inputStartPickingDate, inputEndPickingDate));
        @SuppressWarnings("unchecked")
        ArrayList<Map<String, String>> receiveOrderInfoList = (ArrayList<Map<String, String>>) orderInfoResponse
                .get("data");

        // 空チェック
        if (!CollectionUtils.isEmpty(receiveOrderInfoList)) {
            // 受注IDをリストに格納する
            for (Map<String, String> receiveOrderInfo : receiveOrderInfoList) {
                orderNumberSet.add(receiveOrderInfo.get("receive_order_id"));
            }
        }
        return orderNumberSet;
    }

    /**
     * 受注情報取得API用のパラメータ設定
     * 
     * @param inputStartPickingDate
     * @param inputEndPickingDate
     * @return
     */
    private HashMap<String, String> createReceiveOrderApiParam(String inputStartPickingDate,
            String inputEndPickingDate) {
        HashMap<String, String> apiParams = new HashMap<>();

        // 出荷予定日（開始）以上
        apiParams.put("receive_order_send_plan_date-gte", inputStartPickingDate);
        // 出荷予定日（終了）以下
        apiParams.put("receive_order_send_plan_date-lte", inputEndPickingDate);
        // 有効な伝票
        apiParams.put("receive_order_cancel_date-null", "");
        // 店舗は通販管理部門のみ
        apiParams.put("receive_order_shop_id-in", String.join(",", Constant.NE_SHOP_CODE_RAKUTEN,
                Constant.NE_SHOP_CODE_AMAZON,
                Constant.NE_SHOP_CODE_YAHOO,
                Constant.NE_SHOP_CODE_OFFICIAL));
        apiParams.put("fields", "receive_order_id,receive_order_send_date,receive_order_send_plan_date");

        return apiParams;
    }

    /**
     * 商品ランキングリスト取得
     * 
     * @param _request
     * @param orderNumberList
     * @return
     */
    private List<ItemInfo> getItemRankingList(HttpServletRequest _request, Set<String> orderNumberList) {
        List<ItemInfo> itemRankingList = new ArrayList<ItemInfo>();

        Map<String, ItemInfo> itemInfoMap = new HashMap<String, ItemInfo>();

        // 受注明細APIから明細情報を取得
        HashMap<String, Object> receiveOrderRowInfoResponse = neApiExecute(getCurrentToken(_request),
                NeApiURL.RECEIVEORDER_ROW_SEARCH_PATH, createReceiveOrderRowApiParam(orderNumberList));
        @SuppressWarnings("unchecked")
        ArrayList<Map<String, String>> receiveOrderRowInfoList = (ArrayList<Map<String, String>>) receiveOrderRowInfoResponse
                .get("data");

        // 対象期間の商品リストを作成（商品コード、商品名、商品受注数量,受注金額を保持）
        if (receiveOrderRowInfoList != null) {
            for (Map<String, String> receiveOrderRowInfo : receiveOrderRowInfoList) {
                if (itemInfoMap.containsKey(receiveOrderRowInfo.get("receive_order_row_goods_id"))) {
                    ItemInfo itemInfo = itemInfoMap.get(receiveOrderRowInfo.get("receive_order_row_goods_id"));
                    itemInfo.setShippingAmount(itemInfo.getShippingAmount()
                            + Long.valueOf(receiveOrderRowInfo.get("receive_order_row_quantity")));
                    itemInfo.setSubTotalPrice(itemInfo.getSubTotalPrice()
                            + Double.valueOf(receiveOrderRowInfo.get("receive_order_row_sub_total_price")).longValue());
                    itemInfoMap.put(receiveOrderRowInfo.get("receive_order_row_goods_id"), itemInfo);
                } else {
                    ItemInfo itemInfo = new ItemInfo();
                    itemInfo.setItemCode(receiveOrderRowInfo.get("receive_order_row_goods_id"));
                    itemInfo.setItemName(receiveOrderRowInfo.get("receive_order_row_goods_name"));
                    itemInfo.setShippingAmount(Long.valueOf(receiveOrderRowInfo.get("receive_order_row_quantity")));
                    itemInfo.setSubTotalPrice(
                            Double.valueOf(receiveOrderRowInfo.get("receive_order_row_sub_total_price")).longValue());
                    itemInfoMap.put(receiveOrderRowInfo.get("receive_order_row_goods_id"), itemInfo);
                }
            }
        }

        // 売上順にソートする
        itemRankingList = itemInfoMap.values().stream()
                .sorted((item1, item2) -> item2.getSubTotalPrice().compareTo(item1.getSubTotalPrice()))
                .collect(Collectors.toList());

        return itemRankingList;
    }

    /**
     * 受注明細API呼び出し用のパラメータ設定
     * 
     * @param orderNumberList
     * @return
     */
    private HashMap<String, String> createReceiveOrderRowApiParam(Set<String> orderNumberList) {
        HashMap<String, String> apiParams = new HashMap<String, String>();

        apiParams.put("receive_order_row_receive_order_id-in",
                orderNumberList.stream().collect(Collectors.joining(",")));
        apiParams.put("receive_order_row_cancel_flag-eq", "0");
        apiParams.put("fields",
                "receive_order_row_receive_order_id,receive_order_row_shop_cut_form_id,receive_order_row_goods_id,receive_order_row_goods_name,receive_order_row_goods_option,receive_order_row_quantity,receive_order_row_sub_total_price");

        return apiParams;

    }

    @Data
    class ItemInfo {
        String itemName;
        String itemCode;
        Long shippingAmount;
        Long subTotalPrice;
    }

}
