package jp.co.kawakyo.nextengineapi.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jp.co.kawakyo.nextengineapi.Entity.CustomerInfoForReceiveOrderForm;
import jp.co.kawakyo.nextengineapi.base.BaseController;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;

@Controller
public class ReceiveOrderController extends BaseController {

	@RequestMapping(value = "/registOrder", method = RequestMethod.GET)
	private String showRegistOrderView(HttpServletRequest _request, HttpServletResponse _response, Model model) {
		return "registOrder";
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
		List<Map<String, String>> orderInfoList = filterOrderInfoList(apiResponse);

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
	private List<Map<String, String>> filterOrderInfoList(HashMap<String, Object> apiResponse) {
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
