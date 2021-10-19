package jp.co.kawakyo.nextengineapi.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import jp.co.kawakyo.nextengineapi.base.BaseController;

@Controller
public class ReceiveOrderController extends BaseController {

	@RequestMapping(value="/registOrder", method = RequestMethod.GET)
	private String showRegistOrderView(HttpServletRequest _request, HttpServletResponse _response, Model model) {
		return "registOrder";
	}
	
}
