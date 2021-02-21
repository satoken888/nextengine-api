package jp.co.kawakyo.nextengineapi.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.buf.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

	@RequestMapping(value="/", method=RequestMethod.GET)
	private String initialView(HttpServletRequest _request,HttpServletResponse _response,Model model) throws NeApiClientException, IOException {

		logger.info("start initialView");

		try {
			//アクセストークンを取得
			HashMap<String, Object> userInfo =  neLogin(_request, _response, authClientProperty.getRedirectUrl());
			if(userInfo != null) {
				saveTokenToSession(_request, new NeToken(),userInfo);
			}
		} catch(Exception e) {
			logger.error("アクセストークン取得エラー",e);
		}
		model.addAttribute("pickingInputForm", new PickingInputForm());
		logger.info("end initialView");
		return "index";
	}

	@RequestMapping(value="/picking", method=RequestMethod.POST)
	private String showPickingData(HttpServletRequest _request,HttpServletResponse _response,Model model,@ModelAttribute PickingInputForm pickingInputForm) {

		String inputPickingDate = pickingInputForm.getInputPickingDate();

		try {
			//指定の出荷予定日以降の受注データを取得
			HashMap<String,Object> receiveOrderInfoResponse = neApiExecute(getCurrentToken(_request), NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH, createReceiveOrderApiParam(inputPickingDate + " 00:00:00"));
			@SuppressWarnings("unchecked")
			ArrayList<Map<String,String>> receiveOrderInfoList = (ArrayList<Map<String, String>>) receiveOrderInfoResponse.get("data");

			HashSet<String> orderIdSet = new HashSet<String>();
			HashMap<String, String> orderIdAndSendPlanDateMap = new HashMap<String,String>();
			HashMap<String,HashMap<String,String>> sendPlanMap = new HashMap<String,HashMap<String,String>>();
			for(Map<String,String> receiveOrderInfo : receiveOrderInfoList) {
				//取得した受注データから受注IDのリスト（重複無し）を作成
				orderIdSet.add(receiveOrderInfo.get("receive_order_id"));
				orderIdAndSendPlanDateMap.put(receiveOrderInfo.get("receive_order_id"), receiveOrderInfo.get("receive_order_send_plan_date"));
				sendPlanMap.put(receiveOrderInfo.get("receive_order_send_plan_date"), new HashMap<String,String>());
			}

			//取得した受注IDの出荷に必要な商品リストを取得
			HashMap<String,Object> receiveOrderRowInfoResponse = neApiExecute(getCurrentToken(_request), NeApiURL.RECEIVEORDER_ROW_SEARCH_PATH, createReceiveOrderRowApiParam(orderIdSet));
			@SuppressWarnings("unchecked")
			ArrayList<Map<String,String>> receiveOrderRowInfoList = (ArrayList<Map<String,String>>) receiveOrderRowInfoResponse.get("data");

			for(Map<String,String> receiveOrderRowInfo : receiveOrderRowInfoList) {
				//受注ID取得
				String orderId = receiveOrderRowInfo.get("receive_order_row_receive_order_id");
				//受注IDに対する出荷予定日取得
				String receiveOrderSendPlanDate = orderIdAndSendPlanDateMap.get(orderId);
				//出荷予定日に対する商品・数量マップを取得
				HashMap<String,String> itemQuantityMap = sendPlanMap.get(receiveOrderSendPlanDate);
				//商品名取得
				String itemName = receiveOrderRowInfo.get("receive_order_row_goods_name");
				//アイテムの必要数量取得
				String itemQuantity = receiveOrderRowInfo.get("receive_order_row_quantity");

				//既に商品データが存在するかによって加算か登録か処理を分ける
				if(itemQuantityMap.get(itemName) == null) {
					itemQuantityMap.put(itemName, itemQuantity);
				} else {
					Long quantity = Long.valueOf(itemQuantityMap.get(itemName)) + Long.valueOf(itemQuantity);
					itemQuantityMap.put(itemName, String.valueOf(quantity));
				}
			}

			//ここまでで受注情報に基づく、出荷品リスト（sendPlanMap）は作成完了

			//Viewに渡すデータを生成する。
			TreeSet<String> sendDateList = new TreeSet<String>(sendPlanMap.keySet());
			TreeSet<String> sendItemList = new TreeSet<String>(getReceiveOrderItemList(sendPlanMap));
			HashMap<String,ArrayList<String>> itemQuantityMap = createItemQuantityMap(sendItemList,sendDateList, sendPlanMap);

			if(receiveOrderInfoResponse != null) {
				model.addAttribute("data", ConvertUtils.convertOb2String(sendPlanMap) + "\r\n日付一覧" + ConvertUtils.convertOb2String(sendDateList) + "\r\nアイテム一覧" + ConvertUtils.convertOb2String(sendItemList));
				model.addAttribute("itemQuantityMap", itemQuantityMap);
				model.addAttribute("sendDateList", sendDateList);
				model.addAttribute("sendItemList", sendItemList);
			}

		} catch(Exception e) {
			logger.error("error", e);
		}
		return "index";
	}

	/**
	 * 受注データ取得パラメータ設定
	 * @return
	 */
	private HashMap<String, String> createReceiveOrderApiParam(String searchDate) {

		HashMap<String, String> apiParams = new HashMap<>();

		apiParams.put("receive_order_send_plan_date-gte", searchDate);
		//1週間後までの出荷予定日のものを検索対称する。
		apiParams.put("receive_order_send_plan_date-lte", ConvertUtils.getDateStringAdded(searchDate, 7));

		apiParams.put("fields", "receive_order_id,receive_order_send_date,receive_order_send_plan_date");

		return apiParams;
	}

	private HashMap<String,String> createReceiveOrderRowApiParam(Set<String> orderIdSet) {

		HashMap<String,String> apiParams = new HashMap<>();

		apiParams.put("receive_order_row_receive_order_id-in", StringUtils.join(orderIdSet));

		apiParams.put("fields", "receive_order_row_receive_order_id,receive_order_row_shop_cut_form_id,receive_order_row_goods_id,receive_order_row_goods_name,receive_order_row_quantity");

		return apiParams;
	}

	private Set<String> getReceiveOrderItemList(HashMap<String,HashMap<String,String>> sendPlanMap) {
		HashSet<String> receiveOrderItemList = new HashSet<>();

		if(sendPlanMap != null && sendPlanMap.size() > 0 ) {
			for(Map<String,String> itemQuantityMap : sendPlanMap.values()) {
				receiveOrderItemList.addAll(itemQuantityMap.keySet());
			}
		}
		return receiveOrderItemList;
	}

	private HashMap<String,ArrayList<String>> createItemQuantityMap(Set<String> itemSet,Set<String> dateSet,HashMap<String,HashMap<String,String>> sendPlanMap) {
		HashMap<String,ArrayList<String>> rtnMap = new HashMap<>();

		for(String itemName : itemSet) {
			ArrayList<String> quantities = new ArrayList<>();
			for(String dateStr : dateSet) {
				quantities.add(sendPlanMap.get(dateStr).get(itemName));
			}
			rtnMap.put(itemName, quantities);
		}

		return rtnMap;
	}
}
