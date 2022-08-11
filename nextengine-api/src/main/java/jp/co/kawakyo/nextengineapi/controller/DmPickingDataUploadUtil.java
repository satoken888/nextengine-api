package jp.co.kawakyo.nextengineapi.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import jp.co.kawakyo.nextengineapi.Entity.DmPickingDataCsvRecord;
import jp.co.kawakyo.nextengineapi.utils.ConvertUtils;

public class DmPickingDataUploadUtil {

	static Logger logger = LoggerFactory.getLogger(DmPickingDataUploadUtil.class);

	/**
	 * アップロードファイルからレコードごとのリストを返す
	 * @param uploadFile
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static List<DmPickingDataCsvRecord> getCSVDataFromDmPickingDataCsvRecord(MultipartFile uploadFile) throws UnsupportedEncodingException, IOException {

		List<DmPickingDataCsvRecord> csvData = new ArrayList<DmPickingDataCsvRecord>();

		//アップロードファイル確認
		if(uploadFile != null && uploadFile.getSize() > 0 ) {
			logger.debug("OriginalFilename ：" + uploadFile.getOriginalFilename());
			logger.debug("Name ：" + uploadFile.getName());
			logger.debug("Size ：" + uploadFile.getSize());

			try(BufferedReader br = new BufferedReader(new InputStreamReader(uploadFile.getInputStream(),"SJIS"))) {
				String line;

				//1行目を省くために1回readLineを空うち
				br.readLine();

				//2行目からループを行う。
				while((line = br.readLine()) != null ) {

					//TODO:本来、純粋なCSVファイルの処理として機能を設けたいところだが、
					//商品名内にカンマが入ってるため、","をセパレーターとして設定。
					//ダブルクォートで囲まれてないＣＳＶの場合は使用できない。
					line = ConvertUtils.trimDoubleQuot(line);
					String[] split = line.split("\",\"");

					//CSVファイルの0番目商品コード
					//CSVファイルの1番目商品名
					//CSVファイルの3番目商品数量
					//をレコードとして登録
					DmPickingDataCsvRecord record = new DmPickingDataCsvRecord(split[0],split[1],Long.valueOf(split[3]));
					csvData.add(record);
				}
			}
		}

		return csvData;
	}

}
