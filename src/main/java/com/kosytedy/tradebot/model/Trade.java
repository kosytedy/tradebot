package com.kosytedy.tradebot.model;

import java.io.IOException;

public class Trade {
	
	static boolean isNextOperationToBuy = true;
	
	static double UPWARD_TREND_THRESHOLD = 2.25;
	static double DIP_THRESHOLD = 2.25;
	
	static double PROFIT_THRESHOLD = 1.25;
	static double STOP_LOSS_THRESHOLD = -2.00;
	
	static double lastOpPrice = 100;
	
	public static void main(String[] args) throws InterruptedException, IOException {
		while(true) {
			attemptToMakeTrade();
			Thread.sleep(1000);
		}
	}
	
	public static void attemptToMakeTrade() throws IOException {
		double currentPrice = Api.getMarketPrice();
		double percentageDiff = (currentPrice - lastOpPrice)/lastOpPrice*100;
		if(isNextOperationToBuy) {
			buy(percentageDiff);
		} else {
			sell(percentageDiff);
		}
	}
	
	private static void sell(double percentageDiff) throws IOException {
		if(percentageDiff >= UPWARD_TREND_THRESHOLD || percentageDiff <= DIP_THRESHOLD) {
			lastOpPrice = Api.placeSellOrder();
			isNextOperationToBuy = true;
		}
	}
	
	private static void buy(double percentageDiff) throws IOException {
		if(percentageDiff >= UPWARD_TREND_THRESHOLD || percentageDiff <= DIP_THRESHOLD) {
			lastOpPrice = Api.placeBuyOrder();
			isNextOperationToBuy = false;
		}
	}
}
