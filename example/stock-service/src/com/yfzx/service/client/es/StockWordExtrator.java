package com.yfzx.service.client.es;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.terracotta.agent.repkg.de.schlichtherle.io.FileInputStream;

public class StockWordExtrator {
	public static void main(String[] args) {
//		Set<String> set = getJoinSet("E:/workspace/Stock_service/src/com/yfzx/service/client/es/dicts/stock");
//		saveFile("E:/workspace/Stock_service/src/com/yfzx/service/client/es/dicts/stock/stock.dict", set);
		Set<String> set = getJoinSet("E:/workspace/Stock_service/src/com/yfzx/service/client/es/dicts/stock/stock.dict");
		saveFile("E:/workspace/Stock_service/src/com/yfzx/service/client/es/dicts/stock/stock1.dict", set);
//		System.out.println(strIsEnglish("abcZ"));
	}

	private static Set<String> getJoinSet(String path) {
		File f = new File(path);
		Set<String> set = new HashSet<String>();
		if(f.isFile()) {
			int num = 0;
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
				num = 0;
				for(String str = reader.readLine(); StringUtils.isNotBlank(str); str = reader.readLine()) {
					if(StringUtils.isNotBlank(str) && str.trim().length() > 1) {
						str = str.trim();
						set.add(str);
						if(str.contains(" ") && ! strIsEnglish(str)) {
							str = str.replaceAll(" ", "");
							set.add(str);
						} else if(str.startsWith("*st") || str.startsWith("*ST")) {
							str = str.replaceAll(" ", "");
							set.add(str);
							str = str.replaceAll("\\*ST", "");
							set.add(str);
						}
					}
					num++;
				}
				reader.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println(f.getName() + "    " + set.size() + "   " + num);
		} else if (f.isDirectory()) {
			File[] files = f.listFiles();
			for(File file : files) {
				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
					int num = 0;
					for(String str = reader.readLine(); StringUtils.isNotBlank(str); str = reader.readLine()) {
						set.add(str);
						num++;
					}
					reader.close();
					System.out.println(file.getName() + "    " + set.size() + "   " + num);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return set;
	}

   private static boolean strIsEnglish(String word) {
	   if(StringUtils.isBlank(word)) {
		   return true;
	   }
	   word = word.replaceAll(" ", "");
       for (int i = 0; i < word.length(); i++) {
            if (!(word.charAt(i) >= 'A' && word.charAt(i) <= 'Z')
                    && !(word.charAt(i) >= 'a' && word.charAt(i) <= 'z')) {
                return false;
            }
        }
        return true;
    }

	private static void saveFile(String path, Set<String> set) {
		try {
			if(set != null && set.size() > 0) {
				FileWriter writer = new FileWriter(new File(path), true);
				for(String str : set) {
					writer.write(str + "\r\n");
				}
				writer.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
