package com.sribasu.tradebot;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerodhatech.kiteconnect.KiteConnect;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.utils.Constants;
import com.zerodhatech.models.Holding;
import com.zerodhatech.models.Order;
import com.zerodhatech.models.OrderParams;

@Service
public class TradeService {
	@Autowired
	private AuthService authService;

	public void showPortfolio() throws Exception, KiteException {
		KiteConnect kiteConnect = authService.getKiteConnect();

		// Get margins returns margin model, you can pass equity or commodity as
		// arguments to get margins of respective segments.
		List<Holding> holdings = kiteConnect.getHoldings();

		DecimalFormat df = new DecimalFormat("#.00");
		df.setRoundingMode(RoundingMode.HALF_UP);

		System.out.println(Ansi.ansi().fgBlue()
				.a(getFixedLengthString("SCRIP", 0) + getFixedLengthString("QUANTITY", 0)
						+ getFixedLengthString("BUY PRICE", 0) + getFixedLengthString("MKT PRICE", 0)
						+ getFixedLengthString("PROFIT", 0)));
		if (holdings != null && holdings.size() > 0) {
			for (Holding holding : holdings) {

				System.out.println(formatOutputValue(
						formatOutputValue(
								formatOutputValue(
										formatOutputValue(formatOutputValue(Ansi.ansi(), holding.tradingSymbol, "", ""),
												holding.quantity, "", ""),
										df.format(holding.averagePrice), "", "/-"),
								df.format(holding.lastPrice), "", "/-"),
						df.format(((holding.lastPrice - holding.averagePrice) / holding.averagePrice) * 100), "", "%")
								.reset());
			}
		} else {
			System.out.println(Ansi.ansi().fgRgb(176, 160, 160).a("No holding available").reset());
		}

	}

	public void showOrders() throws Exception, KiteException {
		KiteConnect kiteConnect = authService.getKiteConnect();

		// Get margins returns margin model, you can pass equity or commodity as
		// arguments to get margins of respective segments.
		List<Order> orders = kiteConnect.getOrders();
		DecimalFormat df = new DecimalFormat("#.00");
		df.setRoundingMode(RoundingMode.HALF_UP);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM HH:mm:ss");

		System.out.println(Ansi.ansi().fgBlue()
				.a(getFixedLengthString("SCRIP", 0) + getFixedLengthString("TYPE", 0, 10)
						+ getFixedLengthString("PRICE", 0) + getFixedLengthString("QUANTITY", 0, 10)
						+ getFixedLengthString("STATUS", 0) + getFixedLengthString("EXCHANGE", 0, 10)
						+ getFixedLengthString("ORDERID", 0, 20) + getFixedLengthString("TIME", 0, 20)));
		if (orders != null && orders.size() > 0) {
			for (Order order : orders) {
				double orderPrice = Double.parseDouble(order.price);
				System.out
						.println(
								formatOutputValue(
										formatOutputValue(
												formatOutputValue(
														formatOutputValue(formatOutputValue(
																formatOutputValue(
																		formatOutputValue(
																				formatOutputValue(Ansi.ansi(),
																						order.tradingSymbol, "", ""),
																				order.orderType, "", "", 10),
																		df.format(orderPrice), "", "/-"),
																order.quantity, "", "", 10), order.status, "", ""),
														order.exchange, "", "", 10),
												order.orderId, "", "", 20),
										sdf.format(order.orderTimestamp), "", "", 20).reset());
			}
		} else {
			System.out.println(Ansi.ansi().fgRgb(176, 160, 160).a("No order available").reset());
		}

	}

	public void placeOrder(String transactionType, String symbol, String exchange, int qty, double price, int variety,
			int validity, int orderType, int product, double triggerPrice, double stopLoss)
			throws Exception, KiteException {
		KiteConnect kiteConnect = authService.getKiteConnect();

		String[] varieties = new String[] { Constants.VARIETY_REGULAR, Constants.VARIETY_AMO, Constants.VARIETY_BO,
				Constants.VARIETY_CO };
		String[] validities = new String[] { Constants.VALIDITY_DAY, Constants.VALIDITY_IOC, Constants.VALIDITY_TTL };
		String[] orderTypes = new String[] { Constants.ORDER_TYPE_LIMIT, Constants.ORDER_TYPE_MARKET,
				Constants.ORDER_TYPE_SL, Constants.ORDER_TYPE_SLM };
		String[] products = new String[] { Constants.PRODUCT_CNC, Constants.PRODUCT_MIS, Constants.PRODUCT_MTF,
				Constants.PRODUCT_NRML };

		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");

		symbol = symbol.toUpperCase();
		OrderParams orderParams = new OrderParams();
		orderParams.quantity = qty;
		orderParams.orderType = orderTypes[orderType - 1];
		orderParams.tradingsymbol = symbol;
		orderParams.product = products[product - 1];
		orderParams.exchange = exchange.equalsIgnoreCase("NSE") ? Constants.EXCHANGE_NSE : Constants.EXCHANGE_BSE;
		orderParams.transactionType = transactionType;
		orderParams.validity = validities[validity - 1];
		orderParams.price = price;
		if (triggerPrice > 0) {
			orderParams.triggerPrice = triggerPrice;
		}
		if (stopLoss > 0) {
			orderParams.stoploss = stopLoss;
		}
		orderParams.tag = (symbol.length() > 5 ? symbol.substring(0, 5) : symbol) + sdf.format(new Date());
		Order order = kiteConnect.placeOrder(orderParams, varieties[variety - 1]);
		System.out.print(
				Ansi.ansi().fgBrightGreen().a("Order placed successfully! Order ID: " + order.orderId + "\n").reset());
		showOrders();
	}

	public void watch(String symbol) {
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			// Step 1: Search for full symbol
			ObjectMapper mapper = new ObjectMapper();

			// Step 2: Get quote data
			String quoteUrl = "https://query1.finance.yahoo.com/v8/finance/chart/" + symbol + "?region=US&lang=en-US&includePrePost=false&interval=2m&useYfid=true&range=1d&corsDomain=finance.yahoo.com&.tsrc=finance";
			HttpGet quoteRequest = new HttpGet(quoteUrl);
			CloseableHttpResponse quoteResponse = client.execute(quoteRequest);
			String quoteJson = EntityUtils.toString(quoteResponse.getEntity());
			Map stockDetails = mapper.readValue(quoteJson, LinkedHashMap.class);
			System.out.print("\rShare Price: " + PropertyUtils.getProperty( stockDetails,"chart.(result)[0].meta.regularMarketPrice") );
	        System.out.flush(); // Ensure the output is immediately written to the console
	        Thread.sleep(1000);
	        watch(symbol);
		} catch (Exception e) {
			System.err.println("Error fetching stock data: " + e.getMessage());
		}
	}

	private Ansi formatOutputValue(Ansi predecessor, Object valObj, String prefix, String suffix) {
		return formatOutputValue(predecessor, valObj, prefix, suffix, 15);
	}

	private Ansi formatOutputValue(Ansi predecessor, Object valObj, String prefix, String suffix, int colLength) {
		if (predecessor == null) {
			predecessor = Ansi.ansi();
		}
		prefix = prefix != null ? prefix : "";
		suffix = suffix != null ? suffix : "";
		String value = valObj != null ? String.valueOf(valObj).trim() : "";
		if (StringUtils.isNotBlank(prefix)) {
			predecessor = predecessor.fgBlue().a(prefix);
		}
		if (NumberUtils.isParsable(value)) {
			try {
				Number n = NumberFormat.getInstance().parse(value);
				if (n.doubleValue() < 0.0) {
					return predecessor.fgBrightRed()
							.a(getFixedLengthString(value + suffix, prefix.length(), colLength));
				}
			} catch (ParseException e) {
			}
			return predecessor.fgBrightGreen().a(getFixedLengthString(value + suffix, prefix.length(), colLength));
		} else {
			return predecessor.fgBrightCyan().a(getFixedLengthString(value + suffix, prefix.length(), colLength));
		}
	}

	private String getFixedLengthString(String value, int prefixLength) {
		return getFixedLengthString(value, prefixLength, 15);
	}

	private String getFixedLengthString(String value, int prefixLength, int maxLen) {
		if (prefixLength >= maxLen) {
			return "";
		}
		return StringUtils.rightPad(
				(value.length() > maxLen - prefixLength ? value.substring(0, (maxLen - prefixLength) - 3) + "..."
						: value),
				maxLen - prefixLength, " ");
	}

}
