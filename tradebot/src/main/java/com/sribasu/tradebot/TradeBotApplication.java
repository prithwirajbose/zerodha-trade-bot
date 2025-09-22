package com.sribasu.tradebot;

import java.util.Scanner;

import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.zerodhatech.kiteconnect.kitehttp.exceptions.KiteException;
import com.zerodhatech.kiteconnect.kitehttp.exceptions.TokenException;
import com.zerodhatech.kiteconnect.utils.Constants;

@SpringBootApplication
public class TradeBotApplication implements CommandLineRunner {
	@Autowired
	private TradeService tradeService;

	@Autowired
	private AuthService authService;

	public static void main(String[] args) {
		AnsiConsole.systemInstall();
		SpringApplication.run(TradeBotApplication.class, args);
		AnsiConsole.systemUninstall();
	}

	@Override
	public void run(String... args) {
		try {
			Scanner scanner = new Scanner(System.in);
			/*
			 * System.out.println(Ansi.ansi().fgBrightCyan().a(
			 * "\n*********************************\n*********************************\n****                         ****"
			 * +
			 * "\n****      TRADE BOT 1.0      ****\n****                         ****\n*********************************"
			 * + "\n*********************************") .reset());
			 */
			printLogo();
			String[] supportedCommands = new String[] { "config", "exit", "portfolio", "buy", "sell", "orders", "watch",
					"logout" };
			while (true) {
				try {
					System.out.print(Ansi.ansi().fgBrightYellow()
							.a("\nSupported Commands: " + String.join(", ", supportedCommands) + "\nEnter Command: ")
							.reset());
					if (!scanner.hasNextLine()) {
						System.out.print(Ansi.ansi().fgBrightMagenta().a("\nGoodbye!").reset());
						break;
					}
					String input = scanner.nextLine();
					input = input != null ? input.trim() : "";
					if (authService.isKiteConfigAvailable() || supportedCommands[0].equalsIgnoreCase(input)
							|| supportedCommands[1].equalsIgnoreCase(input)) {
						if (supportedCommands[0].equalsIgnoreCase(input)) {
							System.out.println(Ansi.ansi().fgBrightYellow().a(
									"You need a Zerodha Kite Connect API Key/Secret. If you do not have one already, get it here:\n")
									.reset().fgBrightBlue().a("https://zerodha.com/products/api/").reset());
							String apiKey = "", apiSecret = "", userId = "";

							System.out.print(Ansi.ansi().fgBrightYellow().a("Enter API Key: ").reset());
							if (scanner.hasNextLine())
								apiKey = scanner.nextLine();
							else
								break;

							System.out.print(Ansi.ansi().fgBrightYellow().a("Enter API Secret: ").reset());
							if (scanner.hasNextLine())
								apiSecret = scanner.nextLine();
							else
								break;

							System.out
									.print(Ansi.ansi().fgBrightYellow().a("Enter Your Kite Trading UserID: ").reset());
							if (scanner.hasNextLine())
								userId = scanner.nextLine();
							else
								break;
							authService.saveConfig(apiKey.trim(), apiSecret.trim(), userId.trim());
						} else if (supportedCommands[1].equalsIgnoreCase(input)) {
							System.out.println(Ansi.ansi().fgBrightGreen().a("Goodbye!").reset());
							break;
						} else if (supportedCommands[2].equalsIgnoreCase(input)) {
							tradeService.showPortfolio();
						} else if (input.trim().toLowerCase().startsWith(supportedCommands[3])
								|| input.trim().toLowerCase().startsWith(supportedCommands[4])) {
							String[] params = input.trim().split(" ");
							String transactionType = params != null && params.length >= 1
									&& params[0].trim().equalsIgnoreCase("buy") ? Constants.TRANSACTION_TYPE_BUY
											: Constants.TRANSACTION_TYPE_SELL;
							String symbol = params != null && params.length >= 2 ? params[1].trim() : null;
							String exchange = params != null && params.length >= 3 ? params[2].trim() : null;
							int qty = params != null && params.length >= 4 ? Integer.parseInt(params[3].trim()) : 0;
							double price = params != null && params.length >= 5 ? Double.parseDouble(params[4].trim())
									: 0.0;
							double triggerPrice = params != null && params.length >= 6
									? Double.parseDouble(params[5].trim())
									: -1.0;
							double stopLoss = params != null && params.length >= 7
									? Double.parseDouble(params[6].trim())
									: -1.0;
							int variety = params != null && params.length >= 8 ? Integer.parseInt(params[7].trim())
									: 1;
							int validity = params != null && params.length >= 9 ? Integer.parseInt(params[8].trim())
									: 1;
							int orderType = params != null && params.length >=10 ? Integer.parseInt(params[9].trim())
									: 1;
							int product = params != null && params.length >= 11 ? Integer.parseInt(params[10].trim())
									: 1;
							if (StringUtils.isBlank(symbol)) {
								System.out
										.print(Ansi.ansi().fgBrightYellow().a("Enter Stock Symbol (Scrip): ").reset());
								if (scanner.hasNextLine())
									symbol = scanner.nextLine();
								else
									break;
							}
							if (StringUtils.isBlank(exchange)) {
								System.out.print(
										Ansi.ansi().fgBrightYellow().a("Enter Exchange Name (BSE, NSE): ").reset());
								if (scanner.hasNextLine())
									exchange = scanner.nextLine();
								else
									break;
							}
							if (qty <= 0) {
								System.out.print(Ansi.ansi().fgBrightYellow().a("Enter Quantity: ").reset());
								if (scanner.hasNextLine())
									qty = Integer.parseInt(scanner.nextLine().trim());
								else
									break;
							}
							if (price <= 0.0) {
								System.out.print(Ansi.ansi().fgBrightYellow().a("Enter Price: ").reset());
								if (scanner.hasNextLine())
									price = Double.parseDouble(scanner.nextLine().trim());
								else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow().a("Enter Trigger Price: ").reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										triggerPrice = Double.parseDouble(paramVal.trim());
								} else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow().a("Enter Stop Loss Price: ").reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										stopLoss = Double.parseDouble(paramVal.trim());
								} else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow()
										.a("Enter Variety ([1]: " + Constants.VARIETY_REGULAR + ", 2: "
												+ Constants.VARIETY_AMO + ", 3: " + Constants.VARIETY_BO + ", 4: " + Constants.VARIETY_CO + "): ")
										.reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										variety = Integer.parseInt(paramVal.trim());
								} else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow()
										.a("Enter Validity ([1]: " + Constants.VALIDITY_DAY + ", 2: "
												+ Constants.VALIDITY_IOC + ", 3: " + Constants.VALIDITY_TTL + "): ")
										.reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										validity = Integer.parseInt(paramVal.trim());
								} else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow()
										.a("Enter Order Type ([1]: " + Constants.ORDER_TYPE_LIMIT + ", 2: "
												+ Constants.ORDER_TYPE_MARKET + ", 3: " + Constants.ORDER_TYPE_SL
												+ ", 4: " + Constants.ORDER_TYPE_SLM + "): ")
										.reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										orderType = Integer.parseInt(paramVal.trim());
								} else
									break;
							}
							if (params.length < 4) {
								System.out.print(Ansi.ansi().fgBrightYellow()
										.a("Enter Product ([1]: " + Constants.PRODUCT_CNC + ", 2: "
												+ Constants.PRODUCT_MIS + ", 3: " + Constants.PRODUCT_MTF + ", 4: "
												+ Constants.PRODUCT_NRML + "): ")
										.reset());
								if (scanner.hasNextLine()) {
									String paramVal = scanner.nextLine();
									if (StringUtils.isNotBlank(paramVal))
										orderType = Integer.parseInt(paramVal.trim());
								} else
									break;
							}
							tradeService.placeOrder(transactionType, symbol, exchange, qty, price, variety, validity, orderType,
									product, triggerPrice, stopLoss);
						} else if (supportedCommands[5].equalsIgnoreCase(input)) {
							tradeService.showOrders();
						} else if (supportedCommands[6].equalsIgnoreCase(input)) {
							System.out.println("");
							tradeService.watch("TCS.NS");
						} else if (supportedCommands[7].equalsIgnoreCase(input)) {
							authService.logout();
						} else {
							System.out.println(Ansi.ansi().fgBrightRed().a("Command not supported!").reset());
						}
					} else {
						System.out.println(Ansi.ansi().fgBrightRed()
								.a("Kite configuration missing! Run 'config' command first.").reset());
					}
				} catch (Throwable ex) {
					if (ex instanceof TokenException) {
						System.out.println(Ansi.ansi().fgBrightRed().a("Login session expired!").reset());
					}
					else if (ex instanceof KiteException) {
						System.out.println(Ansi.ansi().fgBrightRed().a(((KiteException) ex).message).reset());
					} else {
						System.out.println(Ansi.ansi().fgBrightRed().a(ex.getMessage()).reset());
					}
				}
			}
			System.exit(0);
		} catch (Throwable ex) {
			System.out.println(Ansi.ansi().fgBrightRed().a("Good Bye!").reset());
		}
	}

	public void printLogo() {
		System.out.println(Ansi.ansi().fgCyan().a(
				"\r\n\r\n*******************************************************************************************************************\r\n"
						+ "*******************************************************************************************************************\r\n"
						+ "*******************************************************************************************************************\r\n"
						+ "****      _________ _______   _______  ______   _______    ______   _______ _________   __       _______       ****\r\n"
						+ "****      \\TTTTTTT/(RRRRRRR) (AAAAAAA)(DDDDDD\\ (EEEEEEE\\  (BBBBBB\\ (OOOOOOO)\\TTTTTTT/  /11\\     (0000000)      ****\r\n"
						+ "****         )T(   |R(    )R||A(   )A||D(  \\DD)|E(    \\E  |B(   )B)|O(   )O|   )T(     1/)1)    |0(  )00|      ****\r\n"
						+ "****         |T|   |R(____)R||A(___)A||D|   )D||E(__      |B(__/B/ |O|   |O|   |T|       |1|    |0| /000|      ****\r\n"
						+ "****         |T|   |RRRRRRR) |AAAAAAA||D|   |D||EEEE)     |BBBBB(  |O|   |O|   |T|       |1|    |0(/0/)0|      ****\r\n"
						+ "****         |T|   |R(\\R(    |A(   )A||D|   )D||E(        |B(  \\B\\ |O|   |O|   |T|       |1|    |000/ |0|      ****\r\n"
						+ "****         |T|   |R) \\R\\__ |A)   (A||D(__/DD)|E(____/E  |B)___)B)|O(___)O|   |T|     __)1(_ _ |00(__)0|      ****\r\n"
						+ "****         )T(   |/   \\RR/ |/     \\|(DDDDDD/ (EEEEEEE/  |B \\BBB/ (OOOOOOO)   )T(     \\1111/(.)(0000000)      ****\r\n"
						+ "****                                                                                                           ****\r\n"
						+ "*******************************************************************************************************************\r\n"
						+ "*******************************************************************************************************************\r\n"
						+ "******************************                                                       ******************************\r\n"
						+ "*****************************    Z E R O D H A   K I T E   T R A D I N G   T O O L    *****************************\r\n"
						+ "******************************                                                       ******************************\r\n"
						+ "*******************************************************************************************************************\r\n"
						+ "*******************************************************************************************************************\r\n\r\n")
				.reset());
	}
}
