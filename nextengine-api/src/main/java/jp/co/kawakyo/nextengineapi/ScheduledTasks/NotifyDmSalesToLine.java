package jp.co.kawakyo.nextengineapi.ScheduledTasks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jp.co.kawakyo.nextengineapi.service.NeApiClientService;
import jp.co.kawakyo.nextengineapi.utils.LineConnectUtils;
import jp.co.kawakyo.nextengineapi.utils.NeApiURL;

@Component
public class NotifyDmSalesToLine {

    @Autowired
	public NeApiClientService neApiClientService;

    @Scheduled(cron="0 0 19 * * ? ")   //每日19時に実施
    public void execute(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //フォーマット
        // System.out.println("現在時刻：" + df.format(new Date()));

        //TokenForLineBatchプロパティから
        //ネクストエンジンのアクセストークン、リフレッシュトークンを読み込み
        ResourceBundle bundle = ResourceBundle.getBundle("TokenForLineBatch");
        String accessToken = bundle.getString("accessToken");
        String refreshToken = bundle.getString("refreshToken");
        System.out.println("accsessToken:" + accessToken);
        System.out.println("refreshToken:" + refreshToken);

        //ネクストエンジンAPIで受注情報検索を行う（当日受注の検索）
        HashMap<String,Object> res = neApiClientService.neApiExecute(NeApiURL.RECEIVEORDER_BASE_SEARCH_PATH,createApiParamForTodaysOrder() , accessToken, refreshToken);
        ArrayList<Map<String,String>> orderInfoList = (ArrayList<Map<String, String>>) res.get("data");

        //売上金額の取得
        //TODO:要作成

        //アクセストークン、リフレッシュトークンが新たに設定されていた場合、
        //それぞれのトークン情報を更新する。

        





        LineConnectUtils.notifySales("testtesttest佐藤　健太郎\n佐藤ですよ。\n" + df.format(new Date()), "nIaBfZyYtAKUU9ZPq5ENSLLrtgILujN7oVmINGnea0t");
    }

    private HashMap<String,String> createApiParamForTodaysOrder() {
        HashMap<String, String> apiParams = new HashMap<>();

		return apiParams;
    }
}
