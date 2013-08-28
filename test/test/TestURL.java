package com.shopstyle.nutchx.test;

import java.util.regex.Pattern;

public class TestURL
{

    private static Pattern AffiliateParamPattern = Pattern.compile("\\bid=[0-9a-zA-Z\\.\\*]+", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args){
        getFeedUrl("http://linksynergy.jrs5.com/link?id=u.5WQ7Oo5Uo&amp;offerid=195086.1661217&amp;type=15&amp;murl=http://crosset.onward.co.jp/shop/l/goods.html?gid=1661217&amp;cid=waf002_20111006_001","");
    }
    public static void getFeedUrl(String url, String eventCookie)
    {

        // Replace the affiliate ID that was in the feed with the partner-specific one.
        String ourAffiliateId = "u*5WQ7Oo5Uo";
        url = AffiliateParamPattern.matcher(url).replaceFirst("id=" + ourAffiliateId);

        System.out.println( url);
    }

}
