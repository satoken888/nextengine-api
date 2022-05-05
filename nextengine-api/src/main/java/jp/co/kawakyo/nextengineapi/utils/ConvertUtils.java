package jp.co.kawakyo.nextengineapi.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class ConvertUtils {

	private ConvertUtils() {
	}

	/**
	 * Convert Object to Json
	 *
	 * @param ob the object need to be converted to String
	 * @return the string value of the param ob
	 * @throws JsonProcessingException if has errors when converting
	 */
	public static String convertOb2String(Object ob) throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		return om.writerWithDefaultPrettyPrinter().writeValueAsString(ob);
	}

	/**
	 * 日付文字列の増減を行う
	 * @param dateStr 元の日付文字列(yyyy-MM-dd HH:mm:ssを想定)
	 * @param adddays 追加する日数
	 * @return
	 */
	public static String getDateStringAdded(String dateStr,int adddays) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.JAPANESE);
		LocalDateTime ldt = LocalDateTime.parse(dateStr, dtf);
		//日付加算
		LocalDateTime rtnDate = ldt.plusDays(adddays);
		return rtnDate.format(dtf);
	}
	
	/**
	 * 文字列前後のダブルクォーテーションを削除するFunction
	 * @param str 文字列
	 * @return 前後のダブルクォーテーションを削除した文字列
	 */
	public static String trimDoubleQuot(String str) {
	  char c = '"';
	  if(str.charAt(0) == c && str.charAt(str.length()-1) == c) {
	    return str.substring(1, str.length()-1);
	  }else {
	    return str;
	  }
	}
	
}
