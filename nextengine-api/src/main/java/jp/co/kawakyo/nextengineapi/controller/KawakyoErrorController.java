package jp.co.kawakyo.nextengineapi.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.fasterxml.jackson.core.JsonProcessingException;

import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;

@Controller
public class KawakyoErrorController implements ErrorController {

	@RequestMapping(value="/error")
	private String showErrorPage(Model model) throws JsonProcessingException {

		try {
			model.addAttribute("data",ConvertUtils.convertOb2String(model));
		} catch (JsonProcessingException e) {
			// TODO 自動生成された catch ブロック
			model.addAttribute("errorMessage",ConvertUtils.convertOb2String(e));
			e.printStackTrace();
		}
		return "error";
	}

	@Override
	public String getErrorPath() {
		// TODO 自動生成されたメソッド・スタブ
		return "/error";
	}

}
