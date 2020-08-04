package com.kosytedy.tradebot.model;

import java.io.IOException;

public class Trade {
	
	static boolean isNextOperationToBuy = true;
	
	static int TRADE_INTERVAL_IN_SECONDS = 30;
	
	static double UPWARD_TREND_THRESHOLD = 2.25;
	static double DIP_THRESHOLD = 2.25;
	
	static double PROFIT_THRESHOLD = 2.25;
	static double STOP_LOSS_THRESHOLD = -2.00;
	
	static double lastOpPrice = 11249.0;
	
	
	
	public static void main(String[] args) throws InterruptedException, IOException {
		lastOpPrice = Api.getMarketPrice();
		int tradeCount = 1;
		while(true) {
			System.out.println("Attempting to make trade no. "+tradeCount);
			attemptToMakeTrade();
			Thread.sleep(TRADE_INTERVAL_IN_SECONDS * 1000);
			tradeCount++;
		}
	}
	
	public static void attemptToMakeTrade() throws IOException {
		double currentPrice = Api.getMarketPrice();
		
		double percentageDiff = (currentPrice - lastOpPrice) / lastOpPrice * 100;
		if(isNextOperationToBuy) {
			buy(currentPrice, percentageDiff);
		} else {
			sell(currentPrice, percentageDiff);
		}
	}
	
	private static void sell(double currentPrice, double percentageDiff) throws IOException {
		if(percentageDiff >= PROFIT_THRESHOLD || percentageDiff <= STOP_LOSS_THRESHOLD) {
			try {
				lastOpPrice = Api.placeSellOrder(currentPrice);
				isNextOperationToBuy = true;
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private static void buy(double currentPrice, double percentageDiff) throws IOException {
		if(percentageDiff >= UPWARD_TREND_THRESHOLD || percentageDiff <= DIP_THRESHOLD) {
			try {
				lastOpPrice = Api.placeBuyOrder(currentPrice);
				isNextOperationToBuy = false;
			} catch(Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
}
