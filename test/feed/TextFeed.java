package com.shopstyle.nutchx.feed;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.shopstyle.nutchx.FeedParser.RetailerFeed;
import com.shopstyle.util.ConfigProperties;


public class TextFeed implements RetailerFeed
{
	private final BufferedReader file;
	private final String lineDelimiter;
	private final Map attributeMapping;

	private boolean fileClosed;

	public TextFeed(String fileLocation, String delimiter, String attributeMappingKey, int skipLines)
			throws IOException
			{
		file = new BufferedReader(new FileReader(fileLocation));
		while (skipLines > 0) {
			// DCA - read the first line as they are columns
			file.readLine();
			skipLines--;
		}
		fileClosed = false;
		lineDelimiter = delimiter;

		attributeMapping = ConfigProperties.getInstance().getMap(
				"com.shopstyle.nutchx.feed.TextFeed.".concat(attributeMappingKey));

			}

	@Override
	public RetailerProduct nextProduct() throws IOException
	{
		if (!fileClosed) {
			String line = file.readLine();
			int flag=0;
			//			String templine=line;
			//			StringBuilder sbr=new StringBuilder(line);
			if(line!=null && line.contains("\""))
			{
				flag=1;
				line=line.replaceAll("\"\"\",(\\w)", "$1\"\"");
				line = line.replaceAll(Pattern.quote(lineDelimiter)+"(?!(([^\"]*\"){2})*[^\"]*$)", "#");
				System.out.println(line);
				System.exit(0);
				//				Pattern p=Pattern.compile("(\".*?"+Pattern.quote(lineDelimiter)+".*?\")");
				//				Matcher m=p.matcher(templine);
				//				if(m.find())
				//				{
				//					for (int i = 1; i <= m.groupCount(); i++) {
				////						m.replaceAll(m.group(i));
				//						String Temp=m.group(i);
				//						String Temp1=Temp;
				//						Temp=Temp.replaceAll("(,)", " ## ");
				//						line=line.replace((Pattern.quote(Temp1)),(Pattern.quote(Temp)));
				//						
				//					}
				//				}
			}
			////				System.out.println(line);
			////				           System.exit(0);
//			System.out.println(flag);
			if (null != line) {
				String[] productArray = line.split(lineDelimiter);
				if(flag==1)
				{
					for (int i = 0; i < productArray.length; i++) {
						productArray[i]=productArray[i].replaceAll("#", ",");
						productArray[i]=productArray[i].replaceAll("\"", "");
					}
				}
				
				TextProduct product = new TextProduct(productArray, attributeMapping);
				return product;
			}
			else {
				file.close();
				fileClosed = true;
			}
		}

		return null;
	}

}
