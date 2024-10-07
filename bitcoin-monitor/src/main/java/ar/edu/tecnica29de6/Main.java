package ar.edu.tecnica29de6;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

public class Main {

	public static void main(String[] args) {
		String url = "https://api.coindesk.com/v1/bpi/currentprice/BTC.json";
		double maxPrice = 0;
		double minPrice = Double.MAX_VALUE;

		// Cargar maxPrice y minPrice desde el archivo CSV
		try {
			double[] prices = loadMaxMinFromCSV("btc_prices.csv");
			maxPrice = prices[0];
			minPrice = prices[1];
		} catch (IOException e) {
			System.out.println("Error al cargar los precios máximos y mínimos: " + e.getMessage());
		}

		while (true) {
			try {
				// Realizar la solicitud HTTP
				CloseableHttpClient httpClient = HttpClients.createDefault();
				HttpGet request = new HttpGet(url);
				request.setHeader(new BasicHeader("Accept-Language", "en-US"));
				HttpResponse response = httpClient.execute(request);
				ObjectMapper mapper = new ObjectMapper();

				JsonNode rootNode = mapper.readTree(response.getEntity().getContent());
				double btcPrice = rootNode.path("bpi").path("USD").path("rate_float").asDouble();
				String timestamp = LocalDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
						.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

				System.out.println("el precio actual de bitcoin es:$"+btcPrice); 

				// Verificar si el precio es mayor al máximo o menor al mínimo
				if (btcPrice > maxPrice) {
					maxPrice = btcPrice; 
					System.out.println("el precio de bitcoin ha alcanzado un nuevo maximo:$"+maxPrice);
				}

				if (btcPrice < minPrice) {
					minPrice = btcPrice;
					System.out.println("el precio de bitcoin ha alcanzado un nuevo minimo:$"+minPrice);
				}

				// Actualizar el archivo CSV con el nuevo precio
				updateCSV(btcPrice, timestamp);

				// Esperar 1 minuto antes de la próxima solicitud
				Thread.sleep(60000);
			} catch (Exception e) {
				System.out.println("Error: " + e.getMessage());
			}
		}
	}

	// Cargar los valores máximo y mínimo desde el archivo CSV
	private static double[] loadMaxMinFromCSV(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			return new double[] { 0, Double.MAX_VALUE }; // Si el archivo no existe, devolver valores por defecto
		}

		double maxPrice = 0;
		double minPrice = Double.MAX_VALUE;
		try (CSVReader reader = new CSVReader(new FileReader(file))) {
			List<String[]> records;
			try {
				records = reader.readAll();

				for (String[] record : records) {
					if (record.length < 2 || record[1].equals("price"))
						continue; // Omitir encabezados
					double price = Double.parseDouble(record[1]);
					if (price > maxPrice)
						maxPrice = price;
					if (price < minPrice)
						minPrice = price;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CsvException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return new double[] { maxPrice, minPrice };
		}
	}


	// Actualizar el archivo CSV con el nuevo precio
	private static void updateCSV(double price, String timestamp) throws IOException {
	    String fileName = "btc_prices.csv";
	    File file = new File(fileName);

	    // Si el archivo no existe, crear y escribir el encabezado
	    if (!file.exists()) {
	        try (CSVWriter writer = new CSVWriter(new FileWriter(fileName))) {
	            writer.writeNext(new String[]{"timestamp", "price"});
	        }
	    }

	    // Crear un DecimalFormat con símbolos específicos para el formato en inglés (punto como separador decimal)
	    DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
	    DecimalFormat df = new DecimalFormat("0.0000", symbols);

	    // Escribir la nueva fila con el timestamp y el precio formateado
	    try (CSVWriter writer = new CSVWriter(new FileWriter(fileName, true))) {
	        writer.writeNext(new String[]{timestamp, df.format(price)});
	    }
	}

}
