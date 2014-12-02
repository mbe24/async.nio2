/*
 * Copyright 2014 Mikael Beyene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package async.nio2;

import static async.nio2.Client.newClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Main {

	private static int PORT = 24089;
	private static int NO_CLIENTS = 10;
	public static int NO_SAMPLES = 100;

	public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {

		if (args.length == 3) {
			PORT = Integer.valueOf(args[0]);
			NO_CLIENTS = Integer.valueOf(args[1]);
			NO_SAMPLES = Integer.valueOf(args[2]);
		}

		if (PORT < 0) {
			System.err.println("Error: port < 0");
			System.exit(1);
		}

		if (NO_CLIENTS < 1) {
			System.err.println("Error: #clients < 1");
			System.exit(1);
		}

		if (NO_SAMPLES < 1) {
			System.err.println("Error: #samples < 1");
			System.exit(1);
		}
		
		System.out.printf("%03d clients on localhost:%d, %03d runs each. All times in µs.%n", NO_CLIENTS, PORT, NO_SAMPLES);

		AsynchronousChannelGroup groupServer = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));
		AsynchronousChannelGroup groupClient = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));

		InetSocketAddress isa = new InetSocketAddress("localhost", PORT);
		Server server = Server.newInstance(isa, groupServer);
		
		ExecutorService es = Executors.newFixedThreadPool(2);
		
		IntStream.range(0, NO_CLIENTS).unordered().parallel()
		.mapToObj(i -> newClient(isa, groupClient))
		.map(c -> CompletableFuture.supplyAsync(c, es))
		.map(future -> future.join())
		.map(Main::collectStats)
		.map(Main::toEvaluationString)
		.forEach(System.out::println);

		es.shutdown();
		es.awaitTermination(5, TimeUnit.SECONDS);
		
		groupClient.shutdown();
		groupClient.awaitTermination(5, TimeUnit.SECONDS);
		
		server.close();
		groupServer.shutdown();
		groupServer.awaitTermination(5, TimeUnit.SECONDS);
	}
	
	private static DescriptiveStatistics collectStats(Long[] data) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Long l : data)
			stats.addValue(l / 1000d);
		return stats;
	}
	
	private static String toEvaluationString(DescriptiveStatistics stats) {
		return String.format("0.50 Percentile  = %8.2f, "
				+ "0.90 Percentile = %8.2f, "
				+ "0.99 Percentile = %8.2f, "
				+ "min = %8.2f, " + "max = %8.2f",
				stats.getMean(), stats.getPercentile(90),
				stats.getPercentile(99), stats.getMin(),
				stats.getMax());
	}
}