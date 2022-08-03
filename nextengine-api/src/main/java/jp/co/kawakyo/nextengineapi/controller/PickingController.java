package jp.co.kawakyo.nextengineapi.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jp.co.kawakyo.nextengineapi.Entity.PickingInputForm;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.base.NeToken;
import jp.co.kawakyo.nextengineapi.utils.Constant;
import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import jp.nextengine.api.sdk.NeApiClient;
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

			// LINEのバッチ処理用にファイル作成をする
			if (userInfo != null
					&& StringUtils.equals((String) userInfo.get("pic_mail_address"), "ke.sato@ramenkan.com")) {
				Properties neTokenProperties = new Properties();
				neTokenProperties.setProperty("accessToken", (String) userInfo.get(NeApiClient.KEY_ACCESS_TOKEN));
				neTokenProperties.setProperty("refreshToken", (String) userInfo.get(NeApiClient.KEY_REFRESH_TOKEN));
				OutputStream ostream = new FileOutputStream("src/main/resources/TokenForLineBatch.properties");
				OutputStreamWriter osw = new OutputStreamWriter(ostream, "UTF-8");
				neTokenProperties.store(osw, "Comments");
			}
		} catch (Exception e) {
			logger.error("アクセストークン取得エラー", e);
		}

		// 初期表示時の入力フォーム取得のためForm生成
		// ラジオボタンが通販部を初期表示する用に設定
		PickingInputForm pickingInputForm = new PickingInputForm();
		pickingInputForm.setDivShop(Constant.NE_DIV_SHOP_DM);
		pickingInputForm.setDivOutput(Constant.NE_DIV_OUTPUT_SHIPPING);
		model.addAttribute("pickingInputForm", pickingInputForm);

		// 初期表示時のラジオボタン表示を通販部に設定
		logger.info("end initialView");
		return "index";
	}

	@RequestMapping(value = "/picking", method = RequestMethod.POST)
	private String showPickingData(HttpServletRequest _request, HttpServletResponse _response, Model model,
			@ModelAttribute PickingInputForm pickingInputForm,
			@RequestParam("dmPickingFile") MultipartFile uploadFile) {

		// 検索画面の出荷予定日（始め）
		String inputStartPickingDate = pickingInputForm.getInputStartPickingDate();
		// 検索画面の出荷予定日（終わり）
		String inputEndPickingDate = pickingInputForm.getInputEndPickingDate();
		// 店舗区分（通販or本館）
		String divShop = pickingInputForm.getDivShop();
		// 出力区分(出荷確認用or工場発注用)
		String divOutput = pickingInputForm.getDivOutput();
		// 画面表示の文言（主にエラーメッセージに利用）
		String displayMessage = "";

		Map<String, String> orderIdAndSendDateMap = new HashMap<>();
		Set<String> sendDateSet = new HashSet<>();
		Map<String, ArrayList<String>> itemQuantityMap = new HashMap<>();
		ArrayList<Long> countOrder = new ArrayList<Long>();

		createPickingLists(_request, inputStartPickingDate, inputEndPickingDate, divShop, divOutput, displayMessage,
				orderIdAndSendDateMap, sendDateSet, itemQuantityMap, countOrder);
		// if (StringUtils.isEmpty(inputStartPickingDate)) {
		// // 出荷予定日（開始）が未入力の際は入力を促すメッセージを返す。
		// displayMessage = "※出荷予定日の検索開始日を入力してください。";
		// } else {
		// // 画面から入力された出荷予定日をもとに、該当の受注データをAPIから取得する
		// List<Map<String, String>> receiveOrderInfoList =
		// getReceiveOrderInfoList(_request, inputStartPickingDate,
		// inputEndPickingDate, divShop);
		//
		// // 受注データの存在チェック
		// if (!CollectionUtils.isEmpty(receiveOrderInfoList)) {
		//
		// // 取得した受注データから受注IDと出荷予定日をマップとして取り出す
		// orderIdAndSendDateMap = getOrderIdAndSendDateMap(receiveOrderInfoList);
		// // 取得した受注データから出荷予定日のリストを作成する
		// sendDateSet = getSendDateSet(receiveOrderInfoList);
		// // 受注明細APIを呼び出し、それぞれの商品ごとの出荷数リストを作成する。
		// itemQuantityMap = getItemQuantityMap(_request, orderIdAndSendDateMap,
		// sendDateSet);
		//
		// if(StringUtils.equals(divOutput, Constant.NE_DIV_OUTPUT_ORDER)) {
		// //出力区分が工場発注用の場合は商品の構成品でリストを再度作成しなおす。
		// //構成品のデータはConstantクラスを参照
		// try {
		// itemQuantityMap = replaceItemQuantityMapForOrder(itemQuantityMap);
		// } catch (JsonProcessingException e) {
		// // TODO 自動生成された catch ブロック
		// e.printStackTrace();
		// }
		// }
		//
		// // 日別の受注件数データ作成
		// Long totalCount = 0L;
		// for (String sendDate : sendDateSet) {
		// Long count = 0L;
		// for (String orderId : orderIdAndSendDateMap.keySet()) {
		// if (StringUtils.equals(orderIdAndSendDateMap.get(orderId), sendDate)) {
		// count++;
		// }
		// }
		// totalCount += count;
		// countOrder.add(count);
		// }
		// // 合計件数を最後に追加
		// countOrder.add(totalCount);
		//
		// } else {
		// displayMessage = "※出荷データが存在しません。";
		// }
		// }

		// ここからCSVuploadの場合,実施する
		if (uploadFile != null && uploadFile.getSize() > 0) {
			logger.info("CSV処理します。");
		}

		model.addAttribute("message", displayMessage);
		model.addAttribute("itemQuantityMap", itemQuantityMap);
		model.addAttribute("sendDateList", sendDateSet);
		model.addAttribute("countOrder", countOrder);
		return "index";
	}

	/**
	 * ピッキングリスト取得・作成処理
	 * 
	 * @param _request
	 * @param inputStartPickingDate ピッキングリスト取得開始日
	 * @param inputEndPickingDate   ピッキングリスト取得終了日
	 * @param divShop               ショップ区分（通販or本館）
	 * @param divOutput             出力区分（出荷用or工場発注用）
	 * @param displayMessage        画面表示用文言
	 * @param orderIdAndSendDateMap 受注IDと出荷予定日のマップ（本処理にて作成）
	 * @param sendDateSet           出荷予定日のセット(本処理にて作成)
	 * @param itemQuantityMap       商品ごと出荷予定日ごとの出荷量マップ（本処理にて作成）
	 * @param countOrder            日ごとの出荷件数（本処理にて作成）
	 */
	public void createPickingLists(HttpServletRequest _request,
			String inputStartPickingDate,
			String inputEndPickingDate,
			String divShop,
			String divOutput,
			String displayMessage,
			Map<String, String> orderIdAndSendDateMap,
			Set<String> sendDateSet,
			Map<String, ArrayList<String>> itemQuantityMap,
			ArrayList<Long> countOrder) {

		if (StringUtils.isEmpty(inputStartPickingDate)) {
			// 出荷予定日（開始）が未入力の際は入力を促すメッセージを返す。
			displayMessage = "※出荷予定日の検索開始日を入力してください。";
		} else {
			// 画面から入力された出荷予定日をもとに、該当の受注データをAPIから取得する
			List<Map<String, String>> receiveOrderInfoList = getReceiveOrderInfoList(_request, inputStartPickingDate,
					inputEndPickingDate, divShop);

			// 受注データの存在チェック
			if (!CollectionUtils.isEmpty(receiveOrderInfoList)) {

				// 取得した受注データから受注IDと出荷予定日をマップとして取り出す
				orderIdAndSendDateMap = getOrderIdAndSendDateMap(receiveOrderInfoList);
				// 取得した受注データから出荷予定日のリストを作成する
				sendDateSet = getSendDateSet(receiveOrderInfoList);
				// 受注明細APIを呼び出し、それぞれの商品ごとの出荷数リストを作成する。
				itemQuantityMap = getItemQuantityMap(_request, orderIdAndSendDateMap, sendDateSet, divShop);

				if (StringUtils.equals(divOutput, Constant.NE_DIV_OUTPUT_ORDER)) {
					// 出力区分が工場発注用の場合は商品の構成品でリストを再度作成しなおす。
					// 構成品のデータはConstantクラスを参照
					try {
						itemQuantityMap = replaceItemQuantityMapForOrder(itemQuantityMap);
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}

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
				countOrder.add(0,totalCount);

			} else {
				displayMessage = "※出荷データが存在しません。";
			}
		}
	}

	/**
	 * 発送商品リストから構成品商品リストへの変換処理
	 * 
	 * @param itemQuantityMap 発送商品の商品名・日別の発送商品数のリスト(String,ArrayList\<String\>形式)
	 * @return itemQuantityMap 構成品の数量を追加し、既存のセット商品項目を削除したマップ
	 * @throws JsonMappingException
	 * @throws JsonProcessingException
	 */
	private Map<String, ArrayList<String>> replaceItemQuantityMapForOrder(
			Map<String, ArrayList<String>> itemQuantityMap) throws JsonMappingException, JsonProcessingException {

		ObjectMapper mapper = new ObjectMapper();
		// Constantクラスより、構成品商品情報を取得する
		Map<String, Map<String, Long>> conversionMap = mapper.readValue(Constant.JSON_CONVERSION_ITEMQUANTITY,
				new TypeReference<Map<String, Map<String, Long>>>() {
				});

		// 構成品情報のキーセットより、変換対象の商品名リストを取得する。
		// そのリストの数分繰り返し処理をする
		for (String conversionItemName : conversionMap.keySet()) {

			// 発送商品リストに変換対象の商品があるか、無い場合は処理をせずにそのままマップを返す
			if (itemQuantityMap.containsKey(conversionItemName)) {

				// 変換対象商品の出荷日別の数量リストを取得する
				ArrayList<String> itemQuantityList = itemQuantityMap.get(conversionItemName);

				// 変換対象商品の変換先商品目のリストを取得する。
				// 赤ベコ丼黄箱セットなどの場合は、黄箱と赤ベコ丼がこのリストに入っている。
				// リストの分繰り返し処理をする
				for (String conversionToItemName : conversionMap.get(conversionItemName).keySet()) {

					// 構成品の数量を取得する
					Long containItemCount = conversionMap.get(conversionItemName).get(conversionToItemName);

					// 発送商品リストにその構成品商品が存在するかチェック
					if (itemQuantityMap.containsKey(conversionToItemName)) {
						// 存在する場合は既存の出荷数量リストに追加の処理を行う。
						ArrayList<String> addItemQuantityList = itemQuantityMap.get(conversionToItemName);
						int i = 0;
						for (String itemQuantity : itemQuantityList) {
							addItemQuantityList.set(i, String.valueOf(Long.valueOf(addItemQuantityList.get(i))
									+ Long.valueOf(itemQuantity) * containItemCount));
							i++;
						}
					} else {
						// 存在しない場合は新規で出荷数量リストを作成しマップに追加する。
						ArrayList<String> addedItemQuantityList = new ArrayList<String>();
						for (String itemQuantity : itemQuantityList) {
							addedItemQuantityList.add(String.valueOf(Long.valueOf(itemQuantity) * containItemCount));
						}
						itemQuantityMap.put(conversionToItemName, addedItemQuantityList);
					}

				}

				// 構成品への変換処理を行ったら、もとの発送対象尾商品情報はマップから削除する。
				// 例えばレンジ麺6個セットの場合は、レンジ麺の追加処理を行った場合は
				// レンジ麺6個セット自体の行の情報は無くす必要がある
				itemQuantityMap.remove(conversionItemName);
			}
		}

		return itemQuantityMap;
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
	 * @param divShop               店舗区分
	 * @return 商品ごとに出荷予定数を配列としてもったマップ
	 */
	private Map<String, ArrayList<String>> getItemQuantityMap(HttpServletRequest _request,
			Map<String, String> orderIdAndSendDateMap, Set<String> sendDateSet, String divShop) {

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
			// 本館の区分の場合、商品オプションをつけないようにする。
			String itemName = "";
			if (StringUtils.equals(divShop, Constant.NE_DIV_SHOP_HONKAN)) {
				// 本館の場合
				itemName = String.join("", receiveOrderRowInfo.get("receive_order_row_goods_id"), "：",
						receiveOrderRowInfo.get("receive_order_row_goods_name")).trim();
			} else {
				// 本館以外の場合
				itemName = String.join("", receiveOrderRowInfo.get("receive_order_row_goods_id").replace("-oka",""), "：",
						receiveOrderRowInfo.get("receive_order_row_goods_name"),
						getOptionName(receiveOrderRowInfo)).trim();
			}
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

		// 商品名の順序に並び替えをしつつ重複を削除した状態の商品名のセットを取得する。
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
			String inputStartPickingDate, String inputEndPickingDate, String divShop) {

		// 検索開始日に時分秒の表記を追加する
		inputStartPickingDate = inputStartPickingDate + " 00:00:00";
		// 検索終了日が未入力の場合は空文字を、そうでない場合は時分秒の表記を追加する
		inputEndPickingDate = StringUtils.isEmpty(inputEndPickingDate) ? "" : inputEndPickingDate + " 00:00:00";

		// API呼び出し（受注情報検索）
		HashMap<String, Object> receiveOrderInfoResponse = neApiExecute(getCurrentToken(_request),
				NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH,
				createReceiveOrderApiParam(inputStartPickingDate, inputEndPickingDate, divShop));
		ArrayList<Map<String, String>> receiveOrderInfoList = (ArrayList<Map<String, String>>) receiveOrderInfoResponse
				.get("data");
		return receiveOrderInfoList;
	}

	/**
	 * 受注データ取得パラメータ設定
	 * 
	 * @return
	 */
	private HashMap<String, String> createReceiveOrderApiParam(String searchStartDate, String searchEndDate,
			String divShop) {

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

		if (StringUtils.equals(divShop, Constant.NE_DIV_SHOP_DM)) {
			/* 通販部用の店舗指定を追加 */
			apiParams.put("receive_order_shop_id-in", String.join(",", Constant.NE_SHOP_CODE_RAKUTEN,
					Constant.NE_SHOP_CODE_AMAZON,
					Constant.NE_SHOP_CODE_YAHOO,
					Constant.NE_SHOP_CODE_OFFICIAL));
		} else {
			apiParams.put("receive_order_shop_id-in", Constant.NE_SHOP_CODE_HONKAN);
		}

		return apiParams;
	}

	/**
	 * 商品明細オプション名の表示非表示のハンドリングメソッド
	 * 
	 * @param receiveOrderRowInfo 受注明細情報
	 * @return オプション名（表示する場合はそのまま、非表示対象となった場合は空文字を返す）
	 */
	private String getOptionName(Map<String, String> receiveOrderRowInfo) {
		String optionName = "";

		// nullチェック
		if (receiveOrderRowInfo.get("receive_order_row_goods_option") != null) {

			// 非表示対象の文字列をregexに表記
			// TODO:非表示リストを後々DB化か？
			String regex = "未成年者の飲酒|※冷凍便商品";
			// 上記の表記を含むオプション名の場合は空文字のまま返す
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(receiveOrderRowInfo.get("receive_order_row_goods_option"));
			if (!m.find()) {
				// オプション名に上記の表記を含まない場合は商品のオプション名を返す
				optionName = StringUtils.SPACE + receiveOrderRowInfo.get("receive_order_row_goods_option");
			}

			// 包装・のしのオプション記載を削除
			regex = "包装・のし";
			p = Pattern.compile(regex);
			m = p.matcher(optionName);
			if (m.find()) {
				// オプション名に包装・のしがある場合はその表記を削除する
				// optionName = StringUtils.substring(optionName, 0, optionName.indexOf(regex));

				//オプション名を空白で分割
				String[] sepStr = StringUtils.split(optionName, "　");
				String replacedOptionName = "";
				for(String option : sepStr ) {
					//オプション項目1項目ずつに指定の文字列が存在するかチェック。
					//オプション名に指定文字列が無ければ表示するオプション名に採用する。
					m = p.matcher(option);
					if(!m.find()) {
						//オプション名からバリエーションの表記を削除して格納
						replacedOptionName += option.replace("バリエーション：","");
					}
				}
				optionName = StringUtils.SPACE + replacedOptionName;
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
			quantities.add(0,String.valueOf(totalCount));
			rtnMap.put(itemName, quantities);
		}

		return rtnMap;
	}
}
