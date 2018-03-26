package com.lsid.console.util;

import java.awt.image.BufferedImage;

import jp.sourceforge.qrcode.data.QRCodeImage;

public class QRCodeImageImpl implements QRCodeImage { 

	BufferedImage bufImg; 

	public QRCodeImageImpl(BufferedImage bufImg) { 
		this.bufImg = bufImg; 
	} 

	public int getWidth() { 
		return bufImg.getWidth(); 
	} 

	public int getHeight() { 
		return bufImg.getHeight(); 
	} 

	public int getPixel(int x, int y) { 
		return bufImg.getRGB(x, y); 
	} 
}
