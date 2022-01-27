package jp.co.kawakyo.nextengineapi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.kawakyo.nextengineapi.Entity.PickingInputForm;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.base.NeToken;
import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import jp.nextengine.api.sdk.NeApiClientException;

@Controller
public class PickingController extends BaseController {

	@RequestMapping(value = "/", method = RequestMethod.GET)
	private String initialView(HttpServletRequest _request, HttpServletResponse _response, Model model)
			throws NeApiClientException, IOException {

		logger.info("start initialView");

		try {
			// アクセストークンを取得
			HashMap<String, Object> userInfo = neLogin(_request, _response, authClientProperty.getRedirectUrl());
			if (userInfo != null) {
				saveTokenToSession(_request, new NeToken(), userInfo);
			}
		} catch (Exception e) {
			logger.error("アクセストークン取得エラー", e);
		}

		// 初期表示時の入力フォーム取得のためForm生成
		model.addAttribute("pickingInputForm", new PickingInputForm());
		logger.info("end initialView");
		return "index";
	}

	@RequestMapping(value = "/picking", method = RequestMethod.POST)
	private String showPickingData(HttpServletRequest _request, HttpServletResponse _response, Model model,
			@ModelAttribute PickingInputForm pickingInputForm) {

		String inputStartPickingDate = pickingInputForm.getInputStartPickingDate();
		String inputEndPickingDate = pickingInputForm.getInputEndPickingDate();
		String displayMessage = "";
		Map<String, String> orderIdAndSendDateMap = new HashMap<>();
		Set<String> sendDateSet = new HashSet<>();
		Map<String, ArrayList<String>> itemQuantityMap = new HashMap<>();
		ArrayList<Long> countOrder = new ArrayList<Long>();

		if (StringUtils.isEmpty(inputStartPickingDate)) {
			// 出荷予定日（開始）が未入力の際は入力を促すメッセージを返す。
			displayMessage = "※出荷予定日の検索開始日を入力してください。";
		} else {
			// 画面から入力された出荷予定日をもとに、該当の受注データをAPIから取得する
			List<Map<String, String>> receiveOrderInfoList = getReceiveOrderInfoList(_request, inputStartPickingDate,
					inputEndPickingDate);

			// 受注データの存在チェック
			if (!CollectionUtils.isEmpty(receiveOrderInfoList)) {

				// 取得した受注データから受注IDと出荷予定日をマップとして取り出す
				orderIdAndSendDateMap = getOrderIdAndSendDateMap(receiveOrderInfoList);
				// 取得した受注データから出荷予定日のリストを作成する
				sendDateSet = getSendDateSet(receiveOrderInfoList);
				// 受注明細APIを呼び出し、それぞれの商品ごとの出荷数リストを作成する。
				itemQuantityMap = getItemQuantityMap(_request, orderIdAndSendDateMap, sendDateSet);

				// 日別の受注件数データ作成
				Long totalCount = 0L;
				for (String sendDate : sendDateSet) {
					Long count = 0L;
					for (String orderId : orderIdAndSendDateMap.keySet()) {
						if (StringUtils.equals(orderIdAndSendDateMap.get(orderId), sendDate)) {
							count++;
						}
					}
					totalCount += count;
					countOrder.add(count);
				}
				// 合計件数を最後に追加
				countOrder.add(totalCount);

			} else {
				displayMessage = "※出荷データが存在しません。";
			}
		}
		model.addAttribute("message", displayMessage);
		model.addAttribute("itemQuantityMap", itemQuantityMap);
		model.addAttribute("sendDateList", sendDateSet);
		model.addAttribute("countOrder", countOrder);
		return "index";
	}

	/**
	 * 出荷予定日のソート済みのリスト（重複無し）を取得する
	 * 
	 * @param receiveOrderInfoList
	 * @return ソート済みの出荷予定日（重複無し）セット
	 */
	private Set<String> getSendDateSet(List<Map<String, String>> receiveOrderInfoList) {
		TreeSet<String> sendDateSet = new TreeSet<String>();
		for (Map<String, String> receiveOrderInfo : receiveOrderInfoList) {
			sendDateSet.add(receiveOrderInfo.get("receive_order_send_plan_date"));
		}
		return sendDateSet;
	}

	/**
	 * 商品ごとの出荷数リストを取得
	 * 
	 * @param _request              HTTPリクエスト
	 * @param orderIdAndSendDateMap 受注IDと出荷予定日を格納したマップ
	 * @param sendDateSet           出荷予定日のソート済み、重複無しのセット
	 * @return 商品ごとに出荷予定数を配列としてもったマップ
	 */
	private Map<String, ArrayList<String>> getItemQuantityMap(HttpServletRequest _request,
			Map<String, String> orderIdAndSendDateMap, Set<String> sendDateSet) {

		// 出荷予定日ごとの商品出荷数のマップを作成する
		// 出荷予定日をキー、商品名・商品数のマップを値としてもつマップを作成する
		HashMap<String, HashMap<String, String>> sendPlanMap = new HashMap<String, HashMap<String, String>>();
		for (String sendDate : sendDateSet) {
			sendPlanMap.put(sendDate, new HashMap<>());
		}

		// 取得した受注IDの出荷に必要な商品リストを取得
		HashMap<String, Object> receiveOrderRowInfoResponse = neApiExecute(getCurrentToken(_request),
				NeApiURL.RECEIVEORDER_ROW_SEARCH_PATH,
				createReceiveOrderRowApiParam(new ArrayList<String>(orderIdAndSendDateMap.keySet())));
		@SuppressWarnings("unchecked")
		ArrayList<Map<String, String>> receiveOrderRowInfoList = (ArrayList<Map<String, String>>) receiveOrderRowInfoResponse
				.get("data");

		for (Map<String, String> receiveOrderRowInfo : receiveOrderRowInfoList) {
			// 受注ID取得
			String orderId = receiveOrderRowInfo.get("receive_order_row_receive_order_id");
			// 受注IDに対する出荷予定日取得
			String receiveOrderSendPlanDate = orderIdAndSendDateMap.get(orderId);
			// 出荷予定日に対する商品・数量マップを取得
			HashMap<String, String> itemQuantityMap = sendPlanMap.get(receiveOrderSendPlanDate);
			// 商品名取得(オプションもnullでなければ追記する)
			String itemName = String.join("", receiveOrderRowInfo.get("receive_order_row_goods_id"), "：",
					receiveOrderRowInfo.get("receive_order_row_goods_name"),
					getOptionName(receiveOrderRowInfo)).trim();

			// アイテムの必要数量取得
			String itemQuantity = receiveOrderRowInfo.get("receive_order_row_quantity");

			// 既に商品データが存在するかによって加算か登録か処理を分ける
			if (itemQuantityMap.get(itemName) == null) {
				itemQuantityMap.put(itemName, itemQuantity);
			} else {
				Long quantity = Long.valueOf(itemQuantityMap.get(itemName)) + Long.valueOf(itemQuantity);
				itemQuantityMap.put(itemName, String.valueOf(quantity));
			}
		}

		TreeSet<String> itemSet = new TreeSet<>(getReceiveOrderItemList(sendPlanMap));

		return createItemQuantityMap(itemSet, sendDateSet, sendPlanMap);
	}

	/**
	 * 受注IDと紐づく出荷予定日マップ取得
	 * 
	 * @param receiveOrderInfoList APIから取得した受注データ情報
	 * @return 受注ID,出荷予定日を格納したマップ
	 */
	private Map<String, String> getOrderIdAndSendDateMap(List<Map<String, String>> receiveOrderInfoList) {
		Map<String, String> orderIdAndSendDateMap = new HashMap<String, String>();

		for (Map<String, String> receiveOrderInfo : receiveOrderInfoList) {
			// 取得した受注データから受注IDのリスト（重複無し）を作成
			orderIdAndSendDateMap.put(receiveOrderInfo.get("receive_order_id"),
					receiveOrderInfo.get("receive_order_send_plan_date"));
		}

		return orderIdAndSendDateMap;
	}

	/**
	 * 受注データ取得API呼び出し
	 * 
	 * @param _request              トークン情報確認のためのHTTPリクエスト
	 * @param inputStartPickingDate 画面で入力された出荷予定日文字列(yyyy-MM-dd形式)
	 * @return 指定の出荷予定日以降の受注情報
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<Map<String, String>> getReceiveOrderInfoList(HttpServletRequest _request,
			String inputStartPickingDate, String inputEndPickingDate) {

		//検索開始日に時分秒の表記を追加する
		inputStartPickingDate = inputStartPickingDate + " 00:00:00";
		//検索終了日が未入力の場合は空文字を、そうでない場合は時分秒の表記を追加する
		inputEndPickingDate = StringUtils.isEmpty(inputEndPickingDate) ? "" : inputEndPickingDate + " 00:00:00";
		
		HashMap<String, Object> receiveOrderInfoResponse = neApiExecute(getCurrentToken(_request),
				NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH,
				createReceiveOrderApiParam(inputStartPickingDate, inputEndPickingDate));
		ArrayList<Map<String, String>> receiveOrderInfoList = (ArrayList<Map<String, String>>) receiveOrderInfoResponse
				.get("data");
		return receiveOrderInfoList;
	}

	/**
	 * 受注データ取得パラメータ設定
	 * 
	 * @return
	 */
	private HashMap<String, String> createReceiveOrderApiParam(String searchStartDate, String searchEndDate) {

		HashMap<String, String> apiParams = new HashMap<>();

		apiParams.put("receive_order_send_plan_date-gte", searchStartDate);

		if (StringUtils.isEmpty(searchEndDate)) {
			// 1週間後までの出荷予定日のものを検索対象とする。
			apiParams.put("receive_order_send_plan_date-lte", ConvertUtils.getDateStringAdded(searchStartDate, 7));
		} else {
			// 終了日が記入されている場合はそちらを対象にする。
			apiParams.put("receive_order_send_plan_date-lte", searchEndDate);
		}
		apiParams.put("receive_order_cancel_date-null", "");

		apiParams.put("fields", "receive_order_id,receive_order_send_date,receive_order_send_plan_date");

		return apiParams;
	}
	
	/**
	 * 商品明細オプション名の表示非表示のハンドリングメソッド
	 * @param receiveOrderRowInfo 受注明細情報
	 * @return オプション名（表示する場合はそのまま、非表示対象となった場合は空文字を返す）
	 */
	private String getOptionName(Map<String,String> receiveOrderRowInfo) {
		String optionName = "";
		
		//nullチェック
		if(receiveOrderRowInfo.get("receive_order_row_goods_option") != null) {
			
			//非表示対象の文字列をregexに表記
			//TODO:非表示リストを後々DB化か？
			String regex = "未成年者の飲酒|※冷凍便商品";
			//上記の表記を含むオプション名の場合は空文字のまま返す
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(receiveOrderRowInfo.get("receive_order_row_goods_option"));
			if(!m.find()) {
				//オプション名に上記の表記を含まない場合は商品のオプション名を返す
				optionName = StringUtils.SPACE + receiveOrderRowInfo.get("receive_order_row_goods_option");
			}
		}
		
		return optionName;
		
	}

	/**
	 * 受注明細APIの呼び出し時に必要な条件文作成
	 * 
	 * @param orderIdSet
	 * @return
	 */
	private HashMap<String, String> createReceiveOrderRowApiParam(List<String> orderIdSet) {

		HashMap<String, String> apiParams = new HashMap<>();

		apiParams.put("receive_order_row_receive_order_id-in", orderIdSet.stream().collect(Collectors.joining(",")));
		
		apiParams.put("receive_order_row_cancel_flag-eq", "0");
		
		apiParams.put("fields",
				"receive_order_row_receive_order_id,receive_order_row_shop_cut_form_id,receive_order_row_goods_id,receive_order_row_goods_name,receive_order_row_goods_option,receive_order_row_quantity");

		return apiParams;
	}

	/**
	 * sendPlanMapから商品一覧を取得
	 * 
	 * @param sendPlanMap
	 * @return
	 */
	private Set<String> getReceiveOrderItemList(HashMap<String, HashMap<String, String>> sendPlanMap) {
		HashSet<String> receiveOrderItemList = new HashSet<>();

		if (sendPlanMap != null && sendPlanMap.size() > 0) {
			for (Map<String, String> itemQuantityMap : sendPlanMap.values()) {
				receiveOrderItemList.addAll(itemQuantityMap.keySet());
			}
		}
		return receiveOrderItemList;
	}

	/**
	 * 商品名と商品出荷数のマップ作成
	 * 
	 * @param itemSet
	 * @param dateSet
	 * @param sendPlanMap
	 * @return
	 */
	private TreeMap<String, ArrayList<String>> createItemQuantityMap(Set<String> itemSet, Set<String> dateSet,
			HashMap<String, HashMap<String, String>> sendPlanMap) {
		TreeMap<String, ArrayList<String>> rtnMap = new TreeMap<>();

		for (String itemName : itemSet) {
			ArrayList<String> quantities = new ArrayList<>();
			Long totalCount = 0L;
			for (String dateStr : dateSet) {
				String quantityStr = sendPlanMap.get(dateStr).get(itemName);
				if (Objects.isNull(quantityStr)) {
					quantities.add("0");
					// 所定期間の合計ピッキング数を計算する
					totalCount += Long.valueOf(0L);
				} else {
					quantities.add(quantityStr);
					// 所定期間の合計ピッキング数を計算する
					totalCount += Long.valueOf(quantityStr);
				}
			}
			quantities.add(String.valueOf(totalCount));
			rtnMap.put(itemName, quantities);
		}

		return rtnMap;
	}
}
