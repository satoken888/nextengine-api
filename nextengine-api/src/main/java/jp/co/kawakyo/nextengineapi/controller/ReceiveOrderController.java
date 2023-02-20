package jp.co.kawakyo.nextengineapi.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
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
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;
import jp.co.kawakyo.nextengineapi.Entity.RegistOrderInputForm;

@Controller
public class ReceiveOrderController extends BaseController {

	@RequestMapping(value = "/registOrder", method = RequestMethod.GET)
	private String showRegistOrderView(HttpServletRequest _request, HttpServletResponse _response, Model model) {
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

		// 成功であれば、成功のメッセージを出力する
		// 失敗であれば失敗のメッセージとデータをそのまま返す
		String resultMessage = String.valueOf(apiResponse.get("result"));
		if (StringUtils.equals(resultMessage, "success")) {
			model.addAttribute("confirmMessage", "登録成功しました。");
		} else {
			model.addAttribute("alertMessage", "登録失敗しました。再度入力してください。");
			model.addAttribute("registOrderInputForm", registOrderInputForm);
		}

		return "registOrder";
	}

	private HashMap<String, String> createOrderUploadApiParam(RegistOrderInputForm inputForm) {
		HashMap<String, String> result = new HashMap<String, String>();

		result.put("receive_order_upload_pattern_id", "8");
		result.put("data_type_1", "csv");
		result.put("data_1", createUploadCsvStr(inputForm));

		return result;
	}

	private String createUploadCsvStr(RegistOrderInputForm inputForm) {
		String rtn = "店舗伝票番号,受注日,受注郵便番号,受注住所１,受注住所２,受注名,受注名カナ,受注電話番号,受注メールアドレス,発送郵便番号,発送先住所１,発送先住所２,発送先名,発送先カナ,発送電話番号,支払方法,発送方法,商品計,税金,発送料,手数料,ポイント,その他費用,合計金額,ギフトフラグ,時間帯指定,日付指定,作業者欄,備考,商品名,商品コード,商品価格,受注数量,商品オプション,出荷済フラグ,顧客区分,顧客コード,消費税率（%）,のし,ラッピング,メッセージ";

		// 受注番号にいれる日付情報を取得
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		Calendar today = Calendar.getInstance();

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
			rtn += String.join(",", Arrays.asList("order" + sdf.format(today.getTime()),
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

		HashMap<String, String> apiParam = createApiParam(tel, div);
		HashMap<String, Object> apiResponse = neApiExecute(getCurrentToken(_request),
				NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH, apiParam);

		result = createCustomerInfoFromOrderInfo(apiResponse, div);

		return result;
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
