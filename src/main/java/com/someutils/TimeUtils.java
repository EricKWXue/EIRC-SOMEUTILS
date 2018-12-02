package com.someutils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeUtils {
	/**
	 * 将多个以天为单位的连续时间合并
	 * @param timeList
	 * @return
	 * @throws ParseException
	 */
	public static List<Map<String, String>> mergeTime(List<String> timeList) throws ParseException{
		List<Map<String, String>> resultList = new ArrayList<>();
		Calendar calendar = Calendar.getInstance();
		
		Map<String, String> timeRange = new HashMap<>();
		timeRange.put("start", timeList.get(0));
		timeRange.put("end", timeList.get(0));
		
		for (int i = 1; i < timeList.size(); i++) {
			Date date = new SimpleDateFormat("yyyy-MM-dd").parse(timeList.get(i)); 
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_MONTH, -1); 
			
			String beforeDate = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
			if(beforeDate.equals(timeList.get(i-1))){
				timeRange.put("end", timeList.get(i));
			}else{
				resultList.add(timeRange);
				timeRange = new HashMap<>();
				timeRange.put("start", timeList.get(i));
				timeRange.put("end", timeList.get(i));
			}
		}
		resultList.add(timeRange);
		
		return resultList;
	}
	
	public static void main(String[] args) throws ParseException {
		List<String> timeList = new ArrayList<>();
		timeList.add("2018-10-01");
		timeList.add("2018-10-02");
		timeList.add("2018-10-03");
		timeList.add("2018-10-05");
		timeList.add("2018-10-07");
		timeList.add("2018-10-08");
		timeList.add("2018-10-20");
		timeList.add("2018-10-22");
		timeList.add("2018-10-23");
		timeList.add("2018-10-24");
		List<Map<String, String>> mergeTime = TimeUtils.mergeTime(timeList);
		System.out.println(mergeTime);
	}
}
