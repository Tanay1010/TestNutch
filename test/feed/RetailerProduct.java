package com.shopstyle.nutchx.feed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public abstract class RetailerProduct
{
    private static final String emptyString = "";

    private Map setAttributes;
    private Map stripFromValueMap;
    private Map addToValueMap;

    public Map getStripFromValueMap()
    {
        return stripFromValueMap;
    }

    public void setStripFromValueMap(Map stripFromValueMap)
    {
        this.stripFromValueMap = stripFromValueMap;
    }

    public Map getAddToValueMap()
    {
        return addToValueMap;
    }

    public void setAddToValueMap(Map addToValueMap)
    {
        this.addToValueMap = addToValueMap;
    }

    public String getAttribute(String attribute)
    {
        String result = null;
        if (setAttributes != null) {
            result = (String) setAttributes.get(attribute);
        }
        if (null == result) {
            result = getAttributeFromFeed(attribute);
        }
        if (null != result && null != stripFromValueMap && stripFromValueMap.containsKey(attribute)) {
            List stripList = (List) stripFromValueMap.get(attribute);
            Iterator stripIterator = stripList.iterator();
            while (stripIterator.hasNext()) {
                String stripMe = (String) stripIterator.next();
                result = result.replaceAll(stripMe, emptyString);
            }
        }
        if (null != result && null != addToValueMap && addToValueMap.containsKey(attribute)) {
            List addList = (List) addToValueMap.get(attribute);
            Iterator addIterator = addList.iterator();
            while (addIterator.hasNext()) {
                String addMe = (String) addIterator.next();
                result = result.concat(addMe);
            }
        }
        return result;
    }

    protected abstract String getAttributeFromFeed(String attribute);

    public void setAttribute(String attribute, String value)
    {
        if (null == setAttributes) {
            setAttributes = new HashMap();
        }
        setAttributes.put(attribute, value);
    }

    public abstract boolean addProduct();
}
