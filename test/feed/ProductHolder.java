package com.shopstyle.nutchx.feed;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.shopstyle.core.bo.Color;
import com.shopstyle.core.bo.Image;
import com.shopstyle.core.bo.InStock;
import com.shopstyle.core.bo.ProductSize;
import com.shopstyle.importer.ProductBuilder;
import com.shopstyle.importer.ProductBuilder.ProductBuilderException;
import com.shopstyle.nutchx.NutchConstants;

public class ProductHolder
{
	private final ProductBuilder innerProductBuilder;
	private Element innerProductElement;
	private Element categoryElement;

	public ProductHolder()
	{
		innerProductBuilder = new ProductBuilder();
	}

	public ProductHolder(Document doc)
	{
		innerProductBuilder = new ProductBuilder();
		innerProductElement = doc.createElement("Product");
		doc.getFirstChild().appendChild(innerProductElement);
	}

	private Element addToElement(Element parent, String label, String value)
	{
		Element elem = parent.getOwnerDocument().createElement(label);
		if (StringUtils.isNotBlank(value)) {
			elem.setTextContent(value);
		}
		parent.appendChild(elem);

		return elem;
	}

	private Element addUrlToElement(Element parent, String label, String value, String urlType)
	{
		Element elem = parent.getOwnerDocument().createElement(label);
		if (StringUtils.isNotBlank(value)) {
			elem.setTextContent(value);
			if(StringUtils.isNotBlank(urlType))
			{
				elem.setAttribute("type",urlType);
			}
		}
		parent.appendChild(elem);

		return elem;
	}

	public void setFeedRetailerName(String retailer)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.retailer, retailer);
		}
		else {
			innerProductBuilder.setFeedRetailerName(retailer);
		}
	}

	public void setRetailerProductId(String retailerProductId)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.productId, retailerProductId);
		}
		else {
			innerProductBuilder.setRetailerProductId(retailerProductId);
		}
	}

	public void setProductName(String name)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.name, name);
		}
		else {
			innerProductBuilder.setProductName(name);
		}

	}

	public void setRetailerBrand(String brand)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.brand, brand);
		}
		else {
			innerProductBuilder.setRetailerBrand(brand);
		}
	}

	public void addProductUrl(String productUrl, String urlType, String bid)
	{
		if (innerProductElement != null) {
			addUrlToElement(innerProductElement, NutchConstants.productUrl, productUrl,urlType);
		}
		else {
			innerProductBuilder.addProductUrl(productUrl, urlType, bid);
		}
	}

	public void addProductImageUrl(String imageUrl)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.imageURL, imageUrl);
		}
		else {
			innerProductBuilder.addProductImageUrl(imageUrl);
		}
	}

	public void setCurrencyAndPriceRange(String currencyAndPriceRange)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.price, currencyAndPriceRange);
		}
		else {
			innerProductBuilder.setCurrencyAndPriceRange(currencyAndPriceRange);
		}
	}

	public void setSalePriceRange(String salePrice)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.salePrice, salePrice);
		}
		else {
			innerProductBuilder.setSalePriceRange(salePrice);
		}
	}

	public void setDescription(String description)
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.description, description);
		}
		else {
			innerProductBuilder.setDescription(description);
		}
	}

	public void addRetailerCategory(String categoryName)
	{
		if (innerProductElement != null) {
			if (categoryElement == null) {
				categoryElement = addToElement(innerProductElement, NutchConstants.category, null);
			}
			addToElement(categoryElement, NutchConstants.part, categoryName);
		}
		else {
			innerProductBuilder.addRetailerCategory(categoryName);
		}
	}

	public void addProductSize(String sizeName)
	{
		innerProductBuilder.addProductSize(sizeName);
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.size, sizeName);
		}
	}

	public void addColor(Color c)
	{
		innerProductBuilder.addColor(c);
		if (innerProductElement != null) {
			Element color = addToElement(innerProductElement, NutchConstants.color, null);
			String colorName = c.getName();
			addToElement(color, NutchConstants.name, colorName);

			Image pImage = c.getProductImage();
			if (pImage != null && StringUtils.isNotBlank(pImage.getUrl())) {
				addToElement(color, NutchConstants.imageURL, pImage.getUrl());
			}

			Image sImage = c.getSwatchImage();
			if (sImage != null && StringUtils.isNotBlank(sImage.getUrl())) {
				addToElement(color, NutchConstants.swatchURL, sImage.getUrl());
			}
		}
	}

	public void setInStockProductSize(InStock inStock, String sizeName)
	{
		innerProductBuilder.setInStockProductSize(inStock, sizeName);
	}

	public void setInStockColorName(InStock inStock, String colorName)
	{
		innerProductBuilder.setInStockColorName(inStock, colorName);
	}

	public void setInStockSku(InStock inStock, String sku)
	{
		innerProductBuilder.setInStockSku(inStock, sku);
	}

	public void addInStock(InStock inStock)
	{
		if (innerProductElement != null) {
			Element instock = addToElement(innerProductElement, NutchConstants.instock, null);
			String sku = inStock.getSku();
			addToElement(instock, NutchConstants.sku, sku);

			ProductSize size = inStock.getProductSize();
			addToElement(instock, NutchConstants.size, size.getRetailerSize());

			Color color = inStock.getColor();
			addToElement(instock, NutchConstants.color, color.getName());
		}
		else {
			innerProductBuilder.addInStock(inStock);
		}
	}

	public void setExtractDate(String dateString) throws ProductBuilderException
	{
		if (innerProductElement != null) {
			addToElement(innerProductElement, NutchConstants.time, dateString);
		}
		else {
			innerProductBuilder.setExtractDate(dateString);
		}
	}

	public ProductBuilder getProductBuilder()
	{
		return innerProductBuilder;
	}
}
