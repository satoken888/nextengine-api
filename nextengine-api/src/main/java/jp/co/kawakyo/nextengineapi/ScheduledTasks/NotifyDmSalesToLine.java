package jp.co.kawakyo.nextengineapi.ScheduledTasks;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotifyDmSalesToLine {
    @Scheduled(cron="0/20 * *  * * ? ")   //每20秒ごと
    public void execute(){
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); //フォーマット
        System.out.println("現在時刻：" + df.format(new Date()));
    }
}
