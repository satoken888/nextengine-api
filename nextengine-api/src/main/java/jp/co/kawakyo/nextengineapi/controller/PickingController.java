package jp.co.kawakyo.nextengineapi.controller;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.co.kawakyo.nextengineapi.Entity.DmPickingDataCsvRecord;
import jp.co.kawakyo.nextengineapi.Entity.PickingInputForm;
import jp.co.kawakyo.nextengineapi.Entity.PickingTableRecord;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.base.NeToken;
import jp.co.kawakyo.nextengineapi.utils.Constant;
import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;
import jp.co.kawakyo.nextengineapi.utils.NeApiClient;
import jp.co.kawakyo.nextengineapi.utils.NeApiClientException;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import lombok.Data;

@Controller
public class PickingController extends BaseController {

	@GetMapping("/")
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

	@PostMapping("/picking")
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
		// ピッキングファイル入力有無
		boolean pickingFileDiv = false;

		// 出荷予定日指定に対してのピッキング商品検索処理実施
		CreatePickingListsResponse res = createPickingLists(_request, inputStartPickingDate, inputEndPickingDate,
				divShop, divOutput);

		// ここからCSVuploadの場合,実施する
		if (uploadFile != null && uploadFile.getSize() > 0) {
			logger.info("CSV処理します。");
			pickingFileDiv = true;
			// DMとECのピッキングリスト合算用に新たにリストを作成
			ArrayList<PickingTableRecord> replacePickingTableList = new ArrayList<PickingTableRecord>();
			try {
				// CSVファイルデータをエンティティに変換
				List<DmPickingDataCsvRecord> csvFileData = DmPickingDataUploadUtil
						.getCSVDataFromDmPickingDataCsvRecord(uploadFile);
				logger.debug(csvFileData.toString());

				// ＤＭリストとＥＣリストをそれぞれ照らし合わせる
				for (DmPickingDataCsvRecord dmRecord : csvFileData) {
					boolean itemExists = false;
					for (PickingTableRecord ecDataRecord : res.getPickingTableRecordList()) {
						if (StringUtils.equals(ecDataRecord.getItemCode(), dmRecord.getItemCd())) {
							// ＥＣとＤＭのリストの中に合致するものがあった場合フラグを立てる
							itemExists = true;
							// ECとDMのリストを合算してリストに格納する
							replacePickingTableList.add(new PickingTableRecord(dmRecord.getItemCd(),
									dmRecord.getItemName(),
									String.valueOf(
											dmRecord.getItemQuantity() + Long.valueOf(ecDataRecord.getEcTotal())),
									ecDataRecord.getEcTotal(), String.valueOf(dmRecord.getItemQuantity())));
							break;
						}
					}

					if (!itemExists) {
						// 合致する商品が無かった場合
						replacePickingTableList.add(new PickingTableRecord(dmRecord.getItemCd(), dmRecord.getItemName(),
								String.valueOf(dmRecord.getItemQuantity()), "0",
								String.valueOf(dmRecord.getItemQuantity())));
					}
				}

				// ECのリスト分ループしてDMのリストにないが、ECのリストにはあるパターンの商品を詰める
				ArrayList<PickingTableRecord> ecOnlyList = new ArrayList<PickingTableRecord>();
				for (PickingTableRecord ecRecord : res.getPickingTableRecordList()) {
					boolean exists = false;
					for (PickingTableRecord totalRecord : replacePickingTableList) {
						//
						if (StringUtils.equals(totalRecord.getItemCode(), ecRecord.getItemCode())) {
							exists = true;
							break;
						}
					}

					if (!exists) {
						ecRecord.setDmTotal("0");
						ecOnlyList.add(ecRecord);
					}
				}

				// リプレース用のリストにECのみの商品を追加
				replacePickingTableList.addAll(ecOnlyList);
				// 商品コード順に並び替えて、レスポンスのリストを上書きする
				res.setPickingTableRecordList(replacePickingTableList.stream()
						.sorted((e1, e2) -> e1.getItemCode().compareTo(e2.getItemCode())).collect(Collectors.toList()));

			} catch (UnsupportedEncodingException e) {
				logger.error("エンコードエラー");
				logger.error("ファイルを確認してください。");
			} catch (IOException e) {
				logger.error("ファイルエラー");
				logger.error("ファイルを確認してください。");
			}

		}

		model.addAttribute("message", res.getDisplayMessage());
		model.addAttribute("itemQuantityMap", res.getItemQuantityMap());
		model.addAttribute("pickingTableRecordList", res.getPickingTableRecordList());
		model.addAttribute("sendDateList", res.getSendDateSet());
		model.addAttribute("countOrder", res.getCountOrder());
		model.addAttribute("pickingFileInputDiv", pickingFileDiv);
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
	 */
	public CreatePickingListsResponse createPickingLists(HttpServletRequest _request,
			String inputStartPickingDate,
			String inputEndPickingDate,
			String divShop,
			String divOutput) {

		CreatePickingListsResponse res = new CreatePickingListsResponse();
		Map<String, String> orderIdAndSendDateMap = new HashMap<String, String>();
		Set<String> sendDateSet = new HashSet<String>();
		Map<String, ArrayList<String>> itemQuantityMap = new HashMap<String, ArrayList<String>>();
		List<PickingTableRecord> tableRecordList = new ArrayList<PickingTableRecord>();
		ArrayList<Long> countOrder = new ArrayList<Long>();
		String displayMessage = "";

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

				// itemQuantityMapを画面表示用にエンティティ化する
				for (String key : itemQuantityMap.keySet()) {
					PickingTableRecord record = new PickingTableRecord();
					record.setItemCode(StringUtils.split(key, "：", 2)[0]);
					record.setItemName(StringUtils.split(key, "：", 2)[1]);
					record.setTotal(itemQuantityMap.get(key).get(0));
					// アップロード時の対応のためにECトータルを別パラメータにも追加
					record.setEcTotal(itemQuantityMap.get(key).get(0));
					record.setSumList(itemQuantityMap.get(key));
					tableRecordList.add(record);
					itemQuantityMap.get(key).remove(0);
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
				countOrder.add(0, totalCount);

			} else {
				displayMessage = "※出荷データが存在しません。";
			}
		}

		res.setDisplayMessage(displayMessage);
		res.setCountOrder(countOrder);
		// res.setItemQuantityMap(itemQuantityMap);
		res.setPickingTableRecordList(tableRecordList);
		res.setSendDateSet(sendDateSet);

		return res;
	}

	@Data
	class CreatePickingListsResponse {
		private String displayMessage;
		private Map<String, ArrayList<String>> itemQuantityMap;
		private Set<String> sendDateSet;
		private ArrayList<Long> countOrder;
		private List<PickingTableRecord> pickingTableRecordList;
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
			// アイテムの必要数量取得
			String itemQuantity = receiveOrderRowInfo.get("receive_order_row_quantity");
			// 商品コード取得
			String itemCode = receiveOrderRowInfo.get("receive_order_row_goods_id");
			// 商品名取得
			String itemNameFromInfoList = receiveOrderRowInfo.get("receive_order_row_goods_name");

			// 受注IDに対する出荷予定日取得
			String receiveOrderSendPlanDate = orderIdAndSendDateMap.get(orderId);
			// 出荷予定日に対する商品・数量マップを取得
			HashMap<String, String> itemQuantityMap = sendPlanMap.get(receiveOrderSendPlanDate);

			// 商品コードから指定の文字列を削除する。
			// 今後も削除処理はここに追加する。
			itemCode = itemCode.replace("-oka", "");
			itemCode = itemCode.replace("0200-come-", "");

			// 商品名取得(オプションもnullでなければ追記する)
			// 本館の区分の場合、商品オプションをつけないようにする。
			String itemName = "";
			if (StringUtils.equals(divShop, Constant.NE_DIV_SHOP_HONKAN)) {
				// 本館の場合
				itemName = String.join("", itemCode, "：", itemNameFromInfoList).trim();
			} else {
				// 本館以外の場合
				itemName = String.join("", itemCode, "：", itemNameFromInfoList, getOptionName(receiveOrderRowInfo))
						.trim();
			}

			// 既に商品データが存在するかによって加算か登録か処理を分ける
			if (itemQuantityMap.get(itemName) == null) {
				itemQuantityMap.put(itemName, itemQuantity);
			} else {
				Long quantity = Long.valueOf(itemQuantityMap.get(itemName)) + Long.valueOf(itemQuantity);
				itemQuantityMap.put(itemName, String.valueOf(quantity));
			}
		}

		// 商品名の順序に並び替えをしつつ重複を削除した状態の商品名のセットを取得する。
		TreeSet<String> itemSet = new TreeSet<String>(getReceiveOrderItemList(sendPlanMap));

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

				// オプション名を空白で分割
				String[] sepStr = StringUtils.split(optionName, "　");
				String replacedOptionName = "";
				for (String option : sepStr) {
					// オプション項目1項目ずつに指定の文字列が存在するかチェック。
					// オプション名に指定文字列が無ければ表示するオプション名に採用する。
					m = p.matcher(option);
					if (!m.find()) {
						// オプション名からバリエーションの表記を削除して格納
						replacedOptionName += option.replace("バリエーション：", "");
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
			quantities.add(0, String.valueOf(totalCount));
			rtnMap.put(itemName, quantities);
		}

		return rtnMap;
	}
}
