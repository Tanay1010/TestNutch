package com.shopstyle.nutchx.feed;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.nutch.util.StringUtil;

import com.shopstyle.nutchx.NutchConstants;

public class TextProduct extends RetailerProduct
{
    private final String[] product;
    private final Map attributeMapping;

    public TextProduct(String[] product, Map attributeMapping)
    {
        this.product = product;
        this.attributeMapping = attributeMapping;
        Map stripFromValueMap = (Map) this.attributeMapping.get("stripFromValue");
        this.setStripFromValueMap(stripFromValueMap);
        Map addToValueMap = (Map) this.attributeMapping.get("addToValue");
        this.setAddToValueMap(addToValueMap);
    }

    @Override
    protected String getAttributeFromFeed(String attribute)
    {
        String result = null;
        Object attributeMapValue = attributeMapping.get(attribute);
        if (attributeMapValue instanceof List) {
            String combiner = (String) attributeMapping.get(attribute.concat("Combiner"));
            if (StringUtil.isEmpty(combiner)) {
                combiner = ";";
            }
            Iterator indexList = ((List) attributeMapValue).iterator();
            while (indexList.hasNext()) {
                String indexString = (String) indexList.next();
                result = result.concat(combiner).concat(getIndex(indexString));
            }
        }
        else {
            String indexString = (String) attributeMapValue;
            if (NutchConstants.retailer.equals(attribute)) {
                return indexString;
            }
            result = getIndex(indexString);
        }
        return result;
    }

    private String getIndex(String indexString)
    {
        String result = null;
        if (!StringUtil.isEmpty(indexString)) {
            int index = Integer.parseInt(indexString);
            if (index < product.length) {
                result = product[index];
            }
        }
        return result;
    }

    @Override
    public boolean addProduct()
    {
        Map includeList = (Map) attributeMapping.get("includeOnly");
        if (null == includeList) {
            return true;
        }

        Set indexes = includeList.keySet();
        Iterator includeIterator = indexes.iterator();
        while (includeIterator.hasNext()) {
            String indexString = (String) includeIterator.next();
            String attribute = getIndex(indexString);

            List includeListForAttribute = (List) includeList.get(indexString);
            if (includeListForAttribute.contains(attribute)) {
                return true;
            }
        }
        return false;
    }
}
