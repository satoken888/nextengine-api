package jp.co.kawakyo.nextengineapi.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.co.kawakyo.nextengineapi.Entity.CustomerInfoForReceiveOrderForm;
import jp.co.kawakyo.nextengineapi.Entity.ItemInfo;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.base.NeToken;
import jp.co.kawakyo.nextengineapi.utils.KintoneConnect;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import jp.nextengine.api.sdk.NeApiClient;
import jp.co.kawakyo.nextengineapi.Entity.RegistOrderInputForm;
import com.kintone.client.model.record.Record;

@Controller
public class ReceiveOrderController extends BaseController {

	@Autowired
	KintoneConnect kintoneClient;

	// private static int sequence = 1;

	@RequestMapping(value = "/registOrder", method = RequestMethod.GET)
	private String showRegistOrderView(HttpServletRequest _request, HttpServletResponse _response, Model model) {
		model.addAttribute("registOrderInputForm", new RegistOrderInputForm());

		// List<Record> customerInfoList = kintoneClient.getCustomerInfo("0225234354");
		// String name =
		// customerInfoList.stream().findFirst().get().getSingleLineTextFieldValue("name");
		// model.addAttribute("confirmMessage", name);
		return "registOrder";
	}

	@RequestMapping(value = "/registOrder", method = RequestMethod.POST)
	private String registOrder(HttpServletRequest _request, HttpServletResponse _response,
			@ModelAttribute RegistOrderInputForm registOrderInputForm, Model model) {

		// 画面から取得した受注情報を取得

		// APIにわたすデータを生成
		HashMap<String, String> apiParam = createOrderUploadApiParam(registOrderInputForm);
		// API送信（受注情報アップロード）
		HashMap<String, Object> apiResponse = neApiExecute(getCurrentToken(_request),
				NeApiURL.RECEIVEORDER_BASE_UPLOAD_PATH, apiParam);

		// kintoneの顧客情報に追加もしくは更新をかける。
		createOrUpdateCustomerInfo(registOrderInputForm);

		// 成功であれば、成功のメッセージを出力する
		// 失敗であれば失敗のメッセージとデータをそのまま返す
		String resultMessage = String.valueOf(apiResponse.get("result"));
		if (StringUtils.equals(resultMessage, "success")) {
			String orderId = apiParam.get("data_1").split(",")[40]
					.substring(apiParam.get("data_1").split(",")[40].length() - 12);
			model.addAttribute("confirmMessage", "登録成功しました。　受注番号：" + orderId);
			model.addAttribute("registOrderInputForm", reloadBuyerInfoOnly(registOrderInputForm));
		} else {
			model.addAttribute("alertMessage", "登録失敗しました。再度入力してください。");
			model.addAttribute("registOrderInputForm", registOrderInputForm);
		}

		return "registOrder";
	}

	/**
	 * kintoneへの顧客情報追加・更新処理
	 * 
	 * @param registOrderInputForm
	 */
	private void createOrUpdateCustomerInfo(RegistOrderInputForm registOrderInputForm) {
		if (StringUtils.isEmpty(registOrderInputForm.getBuyerKintoneId())) {
			// 購入者がkintoneからの取得情報がない場合、新規登録する。
			kintoneClient.registCustomerInfo(registOrderInputForm.getBuyerName(), registOrderInputForm.getBuyerKana(),
					registOrderInputForm.getBuyerTel(), registOrderInputForm.getBuyerZipcode(),
					registOrderInputForm.getBuyerAddress1(), registOrderInputForm.getBuyerAddress2(),
					registOrderInputForm.getMemo(),
					String.valueOf(Math.round(Long.valueOf(registOrderInputForm.getItemAllPrice()) * 0.01)));
		} else {
			// kintoneからの取得情報がある場合かつ、変更があった場合は更新する
			if (StringUtils.equals(registOrderInputForm.getBuyerInfoChangeFlag(), "1")) {
				// 更新処理
				kintoneClient.updateCustomerInfo(registOrderInputForm.getBuyerKintoneId(),
						registOrderInputForm.getBuyerName(), registOrderInputForm.getBuyerKana(),
						registOrderInputForm.getBuyerTel(), registOrderInputForm.getBuyerZipcode(),
						registOrderInputForm.getBuyerAddress1(), registOrderInputForm.getBuyerAddress2(),
						registOrderInputForm.getMemo(),
						String.valueOf(Long.valueOf(registOrderInputForm.getUsablePoint())
								+ Math.round(Long.valueOf(registOrderInputForm.getItemAllPrice()) * 0.01)));
			}
		}

		if (StringUtils.isEmpty(registOrderInputForm.getDestinationKintoneId())
				&& !StringUtils.equals(registOrderInputForm.getBuyerTel(), registOrderInputForm.getDestTel())) {
			// 送り先情報がkintoneからの取得情報がなく、購入者と送り先が異なる場合、新規登録する
			kintoneClient.registCustomerInfo(registOrderInputForm.getDestName(), registOrderInputForm.getDestKana(),
					registOrderInputForm.getDestTel(), registOrderInputForm.getDestZipCode(),
					registOrderInputForm.getDestAddress1(), registOrderInputForm.getDestAddress2(), "",
					"0");
		} else {
			// kintoneからの取得情報がある場合、かつ、へんこうがあった場合は更新する
			if (StringUtils.equals(registOrderInputForm.getDestinationInfoChangeFlag(), "1")) {
				kintoneClient.updateCustomerInfo(registOrderInputForm.getDestinationKintoneId(),
						registOrderInputForm.getDestName(),
						registOrderInputForm.getDestKana(),
						registOrderInputForm.getDestTel(), registOrderInputForm.getDestZipCode(),
						registOrderInputForm.getDestAddress1(), registOrderInputForm.getDestAddress2(), null, null);
			}
		}
	}

	/**
	 * 登録完了時のinputFormの初期化処理
	 * 購入者情報のみ残すようにする
	 * 
	 * @param registOrderInputForm
	 * @return
	 */
	private RegistOrderInputForm reloadBuyerInfoOnly(RegistOrderInputForm registOrderInputForm) {
		RegistOrderInputForm rtnForm = new RegistOrderInputForm();
		if (StringUtils.isEmpty(registOrderInputForm.getUsablePoint())) {
			registOrderInputForm.setUsablePoint("0");
		}

		rtnForm.setBuyerKintoneId(registOrderInputForm.getBuyerKintoneId());
		rtnForm.setBuyerTel(registOrderInputForm.getBuyerTel());
		rtnForm.setBuyerZipcode(registOrderInputForm.getBuyerZipcode());
		rtnForm.setBuyerAddress1(registOrderInputForm.getBuyerAddress1());
		rtnForm.setBuyerAddress2(registOrderInputForm.getBuyerAddress2());
		rtnForm.setBuyerName(registOrderInputForm.getBuyerName());
		rtnForm.setBuyerKana(registOrderInputForm.getBuyerKana());
		rtnForm.setMemo(registOrderInputForm.getMemo());
		rtnForm.setUsablePoint(String.valueOf(Long.valueOf(registOrderInputForm.getUsablePoint())
				+ Math.round(Long.valueOf(registOrderInputForm.getItemAllPrice()) * 0.01)));
		rtnForm.setReceptionist(registOrderInputForm.getReceptionist());
		rtnForm.setOrderClass(registOrderInputForm.getOrderClass());

		return rtnForm;
	}

	private HashMap<String, String> createOrderUploadApiParam(RegistOrderInputForm inputForm) {
		HashMap<String, String> result = new HashMap<String, String>();

		// 備考欄の文字列を作成する
		inputForm.setRemarks(modifyRemarks(inputForm));

		// TEST用：７番
		// result.put("receive_order_upload_pattern_id", "7");
		// 本番用：８番
		result.put("receive_order_upload_pattern_id", "8");
		result.put("data_type_1", "csv");
		result.put("data_1", createUploadCsvStr(inputForm));

		return result;
	}

	/**
	 * 備考欄の項目の整理
	 *
	 * 画面から入力されたピッキング指示や発送伝票備考欄について、
	 * 受注伝票画面に反映させるために変換
	 * （別途、備考欄変換の設定をNE側に設定済み）
	 *
	 * @param inputForm
	 * @return
	 */
	private String modifyRemarks(RegistOrderInputForm inputForm) {
		String rtn = "";

		if (StringUtils.isNotBlank(inputForm.getWorkerArea()))
			rtn += "【ピッキング指示：" + inputForm.getWorkerArea() + "】";
		if (StringUtils.isNotBlank(inputForm.getInvoiceWrite()))
			rtn += "【発送伝票備考欄：" + inputForm.getInvoiceWrite() + "】";
		if (StringUtils.equals(inputForm.getCoolDiv(), "0")) {
			rtn += "【温度帯：常温】";
		} else if (StringUtils.equals(inputForm.getCoolDiv(), "1")) {
			rtn += "【温度帯：冷蔵】";
		} else if (StringUtils.equals(inputForm.getCoolDiv(), "2")) {
			rtn += "【温度帯：冷凍】";
		}
		if (StringUtils.isNotBlank(inputForm.getShippingSchedule())) {
			rtn += "【出荷予定日：" + inputForm.getShippingSchedule() + "】";
		}
		if (StringUtils.isNotBlank(inputForm.getReceptionist())) {
			rtn += "【担当者：" + inputForm.getReceptionist() + "】";
		}
		if (StringUtils.isNotBlank(inputForm.getOrderClass())) {
			rtn += "【受注区分：" + inputForm.getOrderClass() + "】";
		}

		return rtn;
	}

	private String createUploadCsvStr(RegistOrderInputForm inputForm) {
		String rtn = "店舗伝票番号,受注日,受注郵便番号,受注住所１,受注住所２,受注名,受注名カナ,受注電話番号,受注メールアドレス,発送郵便番号,発送先住所１,発送先住所２,発送先名,発送先カナ,発送電話番号,支払方法,発送方法,商品計,税金,発送料,手数料,ポイント,その他費用,合計金額,ギフトフラグ,時間帯指定,日付指定,作業者欄,備考,商品名,商品コード,商品価格,受注数量,商品オプション,出荷済フラグ,顧客区分,顧客コード,消費税率（%）,のし,ラッピング,メッセージ";

		// 受注番号にいれる日付情報を取得
		SimpleDateFormat sdf = new SimpleDateFormat("yyMMddhhmmss");
		Calendar today = Calendar.getInstance();

		// // ファイルから取得する
		// String sequence = rb.getString("sequence");
		// logger.info("sequence:" + sequence);

		// // ファイルに書き込みをする
		// String sequenceStr = String.valueOf("0000" +
		// String.valueOf(Integer.parseInt(sequence) + 1));
		// Properties sequenceProperties = new Properties();
		// sequenceProperties.setProperty("sequence",
		// sequenceStr.substring(sequenceStr.length() - 5));
		// try (OutputStream ostream = new
		// FileOutputStream("src/main/resources/sequence.properties")) {
		// OutputStreamWriter osw = new OutputStreamWriter(ostream, "UTF-8");
		// sequenceProperties.store(osw, "Comments");
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		// 商品情報をリストで保持
		String[] itemCodeList = inputForm.getItemCode().split(",", -1);
		String[] itemNameList = inputForm.getItemName().split(",", -1);
		String[] itemPriceList = inputForm.getItemPrice().split(",", -1);
		String[] itemCountList = inputForm.getItemCount().split(",", -1);
		String[] itemOptionList = inputForm.getItemOption().split(",", -1);
		String[] itemTaxRateList = inputForm.getItemTaxRate().split(",", -1);

		// 商品の明細数分繰り返す
		for (int i = 0; i < itemCodeList.length; i++) {

			if (StringUtils.isEmpty(itemCodeList[i])) {
				break;
			}
			rtn += "\r\n";
			rtn += String.join(",", Arrays.asList(sdf.format(today.getTime()),
					inputForm.getOrderDate() + StringUtils.SPACE
							+ String.format("%2s", today.get(Calendar.HOUR_OF_DAY)).replace(" ", "0") + ":"
							+ String.format("%2s", today.get(Calendar.MINUTE)).replace(" ", "0") + ":"
							+ String.format("%2s", today.get(Calendar.SECOND)).replace(" ", "0"),
					inputForm.getBuyerZipcode(), inputForm.getBuyerAddress1(),
					inputForm.getBuyerAddress2(),
					inputForm.getBuyerName(), inputForm.getBuyerKana(), inputForm.getBuyerTel(),
					"tuhan@ramenkan.com",
					inputForm.getDestZipCode(), inputForm.getDestAddress1(),
					inputForm.getBuyerAddress2(),
					inputForm.getDestName(), inputForm.getDestKana(), inputForm.getDestTel(),
					inputForm.getPaymentMethod(),
					inputForm.getShippingMethod(), inputForm.getItemAllPrice(),
					inputForm.getTaxPrice(),
					inputForm.getShippingPrice(), inputForm.getCommisionPrice(),
					inputForm.getUsePoint(),
					inputForm.getOtherPrice(), inputForm.getBillingPrice(),
					inputForm.isGiftFlag() ? "1" : "0", inputForm.getShippingTimeZone(), inputForm.getDeliveryDate(),
					inputForm.getWorkerArea(), inputForm.getRemarks(), itemNameList[i], itemCodeList[i],
					itemPriceList[i], itemCountList[i], itemOptionList[i], "0", "0", "",
					itemTaxRateList[i], "", "", ""));
		}

		return rtn;
	}

	/**
	 * Ajax通信用電話番号からの顧客情報取得
	 * 
	 * @param _request httpリクエスト
	 * @param tel      電話番号(購入者または、お送り先様)
	 * @param div      購入者・お送り先様区分（1=購入者,2=お送り先様）
	 * @return 顧客情報リスト（完全同一情報はまとめている）
	 */
	@RequestMapping(value = "/searchCustomer", method = RequestMethod.POST)
	@ResponseBody
	private List<CustomerInfoForReceiveOrderForm> searchCustomerByTel(HttpServletRequest _request,
			@RequestParam String tel, @RequestParam("div") Long div) {
		List<CustomerInfoForReceiveOrderForm> result = new ArrayList<CustomerInfoForReceiveOrderForm>();

		// kintoneより顧客情報取得
		List<Record> customerInfoList = kintoneClient.getCustomerInfo(tel);

		// 画面表示用にオブジェクトを変換する
		result = modifyCustomerInfoList(customerInfoList);
		// for (Record record : customerInfoList) {
		// CustomerInfoForReceiveOrderForm rtnInfo = new
		// CustomerInfoForReceiveOrderForm();
		// rtnInfo.setName(record.getSingleLineTextFieldValue("name"));
		// rtnInfo.setKana(record.getSingleLineTextFieldValue("kana"));
		// rtnInfo.setAddress1(record.getSingleLineTextFieldValue("address1"));
		// rtnInfo.setAddress2(record.getSingleLineTextFieldValue("address2"));
		// rtnInfo.setTel(record.getSingleLineTextFieldValue("tel"));
		// rtnInfo.setFax(record.getSingleLineTextFieldValue("fax"));
		// rtnInfo.setZip_code(record.getSingleLineTextFieldValue("zipcode"));
		// rtnInfo.setMail_address("kintone@ramenkan.com");
		// rtnInfo.setMemo(record.getMultiLineTextFieldValue("memo"));
		// rtnInfo.setUsablePoint(String.valueOf(record.getNumberFieldValue("point")));
		// result.add(rtnInfo);
		// }

		if (result.size() == 0) {
			// NEの受注履歴より顧客情報取得
			HashMap<String, String> apiParam = createApiParam(tel, div);
			HashMap<String, Object> apiResponse = neApiExecute(getCurrentToken(_request),
					NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH, apiParam);

			result = createCustomerInfoFromOrderInfo(apiResponse, div);
		}

		return result;
	}

	private List<CustomerInfoForReceiveOrderForm> modifyCustomerInfoList(List<Record> customerInfoList) {
		List<CustomerInfoForReceiveOrderForm> rtn = new ArrayList<CustomerInfoForReceiveOrderForm>();

		for (Record record : customerInfoList) {
			CustomerInfoForReceiveOrderForm rtnInfo = new CustomerInfoForReceiveOrderForm();
			rtnInfo.setId(String.valueOf(record.getId()));
			rtnInfo.setName(record.getSingleLineTextFieldValue("name"));
			rtnInfo.setKana(record.getSingleLineTextFieldValue("kana"));
			rtnInfo.setAddress1(record.getSingleLineTextFieldValue("address1"));
			rtnInfo.setAddress2(record.getSingleLineTextFieldValue("address2"));
			rtnInfo.setTel(record.getSingleLineTextFieldValue("tel"));
			rtnInfo.setFax(record.getSingleLineTextFieldValue("fax"));
			rtnInfo.setZip_code(record.getSingleLineTextFieldValue("zipcode"));
			rtnInfo.setMail_address("kintone@ramenkan.com");
			rtnInfo.setMemo(record.getMultiLineTextFieldValue("memo"));
			rtnInfo.setUsablePoint(record.getNumberFieldValue("point") == null ? "0"
					: String.valueOf(record.getNumberFieldValue("point")));
			rtn.add(rtnInfo);
		}
		return rtn;
	}

	/**
	 * 商品情報取得API
	 * 
	 * 商品コードからネクストエンジンAPIに検索をかけて、
	 * 商品情報を取得する。
	 * 
	 * @param _request
	 * @param itemCode
	 * @return
	 */
	@RequestMapping(value = "/searchGoods", method = RequestMethod.POST)
	@ResponseBody
	private List<ItemInfo> searchItemByItemCode(HttpServletRequest _request, @RequestParam String itemCode) {
		List<ItemInfo> result = new ArrayList<ItemInfo>();

		HashMap<String, String> apiParam = createApiParamForSearchItem(itemCode);
		HashMap<String, Object> apiResponse = neApiExecute(getCurrentToken(_request), NeApiURL.GOODS_SEARCH_PATH,
				apiParam);

		result = createItemInfoList(apiResponse);

		return result;
	}

	/**
	 * 商品情報取得API用のAPIリクエストパラメータ作成処理
	 * 
	 * @param itemCode
	 * @return
	 */
	private HashMap<String, String> createApiParamForSearchItem(String itemCode) {
		HashMap<String, String> apiParams = new HashMap<String, String>();

		apiParams.put("goods_id-like", itemCode + "%");
		apiParams.put("goods_merchandise_id-eq", "0");
		apiParams.put("fields", "goods_id,goods_name,goods_selling_price,goods_tax_rate");

		return apiParams;
	}

	/**
	 * 商品情報取得APIのレスポンスから商品情報をエンティティにまとめる処理
	 * 
	 * @param apiReponse
	 * @return
	 */
	private List<ItemInfo> createItemInfoList(HashMap<String, Object> apiReponse) {

		List<ItemInfo> itemInfoList = new ArrayList<ItemInfo>();
		// apiのレスポンスからdataを抽出
		List<Map<String, String>> data = filterApiResponse(apiReponse);

		for (Map<String, String> entity : data) {
			ItemInfo itemInfo = new ItemInfo();
			itemInfo.setItemCode(entity.get("goods_id"));
			itemInfo.setItemName(entity.get("goods_name"));
			// itemInfo.setItemOption("");
			itemInfo.setItemPrice(entity.get("goods_selling_price"));
			itemInfo.setItemTaxrate(entity.get("goods_tax_rate"));
			// itemInfo.setItemCount("");
			itemInfoList.add(itemInfo);
		}
		return itemInfoList;
	}

	/**
	 * 受注情報から顧客リストを抽出する
	 * 
	 * @param apiResponse 受注情報が入ったapiレスポンス
	 * @param div         購入者・先様区分（1=購入者、2=先様）
	 * @return
	 */
	private List<CustomerInfoForReceiveOrderForm> createCustomerInfoFromOrderInfo(
			HashMap<String, Object> apiResponse, Long div) {
		HashSet<CustomerInfoForReceiveOrderForm> resultSet = new HashSet<CustomerInfoForReceiveOrderForm>();

		// 受注情報一覧を抽出
		List<Map<String, String>> orderInfoList = filterApiResponse(apiResponse);

		// 顧客情報を取得
		// div=0のときは購入者情報を、
		// div=1のときは先様情報を取得
		if (div == 0) {
			for (Map<String, String> orderInfo : orderInfoList) {
				CustomerInfoForReceiveOrderForm customer = new CustomerInfoForReceiveOrderForm();
				customer.setName(orderInfo.get("receive_order_purchaser_name"));
				customer.setKana(orderInfo.get("receive_order_purchaser_kana"));
				customer.setMail_address(orderInfo.get("receive_order_purchaser_mail_address"));
				customer.setZip_code(orderInfo.get("receive_order_purchaser_zip_code"));
				customer.setAddress1(orderInfo.get("receive_order_purchaser_address1"));
				customer.setAddress2(orderInfo.get("receive_order_purchaser_address2"));
				customer.setTel(orderInfo.get("receive_order_purchaser_tel"));
				customer.setFax(orderInfo.get("receive_order_purchaser_fax"));
				resultSet.add(customer);
			}
		} else if (div == 1) {
			for (Map<String, String> orderInfo : orderInfoList) {
				CustomerInfoForReceiveOrderForm customer = new CustomerInfoForReceiveOrderForm();
				customer.setName(orderInfo.get("receive_order_consignee_name"));
				customer.setKana(orderInfo.get("receive_order_consignee_kana"));
				customer.setMail_address(null);
				customer.setZip_code(orderInfo.get("receive_order_consignee_zip_code"));
				customer.setAddress1(orderInfo.get("receive_order_consignee_address1"));
				customer.setAddress2(orderInfo.get("receive_order_consignee_address2"));
				customer.setTel(orderInfo.get("receive_order_consignee_tel"));
				customer.setFax(orderInfo.get("receive_order_consignee_fax"));
				resultSet.add(customer);
			}
		}
		return new ArrayList<CustomerInfoForReceiveOrderForm>(resultSet);
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, String>> filterApiResponse(HashMap<String, Object> apiResponse) {
		return (List<Map<String, String>>) apiResponse.get("data");
	}

	/**
	 * 電話番号からの顧客情報取得APIのクエリパラメータ生成
	 * 
	 * @param tel
	 * @param div
	 * @return
	 */
	private HashMap<String, String> createApiParam(String tel, Long div) {
		HashMap<String, String> apiParams = new HashMap<String, String>();

		if (div == 0) {
			apiParams.put("receive_order_purchaser_tel-eq", tel);
			apiParams.put("fields",
					"receive_order_purchaser_name,receive_order_purchaser_kana,receive_order_purchaser_zip_code,receive_order_purchaser_address1,receive_order_purchaser_address2,receive_order_purchaser_tel,receive_order_purchaser_fax,receive_order_purchaser_mail_address");

		} else {
			apiParams.put("receive_order_consignee_tel-eq", tel);
			apiParams.put("fields",
					"receive_order_consignee_name,receive_order_consignee_kana,receive_order_consignee_zip_code,receive_order_consignee_address1,receive_order_consignee_address2,receive_order_consignee_tel,receive_order_consignee_fax");
		}
		return apiParams;
	}

}
