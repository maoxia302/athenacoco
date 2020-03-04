package athena.socket.util;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GetUpgradeIpList {

	public static Map<String, String> getIpList() {
		Map<String, String> map = new ConcurrentHashMap<>();
		String path = StringUtils.getFilePath("upgrade_ip.properties");
		try {
			FileInputStream is = new FileInputStream(new File(path));
			Properties p = new Properties();
			p.load(is);
			Set<Object> keySet = p.keySet();
			for (Object aKeySet : keySet) {
				String ipList = (String) p.get(aKeySet);
				String[] ips = ipList.split(";");
				String times = ips[0];
				String[] actualIps = ips[1].split(",");
				if (times != null && !times.equals("")) {
					for (String ip : actualIps) {
						map.put(ip, times);
					}
				} else {
					for (String ip : actualIps) {
						map.put(ip, "");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	public static void timePlay(final String time, final String host) throws ParseException {
		String[] times = time.split("-");
		String startTime = times[0];
		String endTime = times[1];
		Date stDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(startTime);
		final Date ndDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(endTime);
		Timer timer = new Timer(true);
		timer.schedule(new TimerTask() {
			String line = "";
			Timer timerIn = new Timer(true);
			@Override
			public void run() {
				try {
					line = replacing(time);
					timerIn.schedule(new TimerTask() {
						@Override
						public void run() {
							try {
								replacing(line);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}, ndDate);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}, stDate);
	}
	
	private static String replacing(String rep) throws Exception {
		String removed = "";
		String path = StringUtils.getFilePath("upgrade_ip.properties");
		FileInputStream is = new FileInputStream(new File(path));
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);
		String line;
		StringBuilder sb = new StringBuilder();
		FileWriter fw;
		while((line = br.readLine()) != null) {
			if(line.contains(rep)) {
				line = line.replace(rep, "");
				removed = line;
			}
			if(!line.equals("")) {
				sb.append(line).append("\r\n");
			}
		}
		File f = new File(path);
		fw = new FileWriter(f);
		fw.write(sb.toString());
		fw.flush();
		fw.close();
		br.close();
		isr.close();
		is.close();
		return removed;
	}
	
}
