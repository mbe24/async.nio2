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
import static java.util.stream.IntStream.range;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class Main {

	private static int PORT = 0;
	private static int NO_CLIENTS = 50;
	public static int NO_SAMPLES = 1000;

	public static void main(String[] args) throws IOException,
			InterruptedException, ExecutionException {

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


		AsynchronousChannelGroup groupServer = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));
		AsynchronousChannelGroup groupClient = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));

		Server server = Server.newInstance(new InetSocketAddress("localhost", PORT), groupServer);
		InetSocketAddress localAddress = server.getLocalAddress();
		String hostname = localAddress.getHostName();
		int port = localAddress.getPort();
		
		ExecutorService es = Executors.newFixedThreadPool(2);

		System.out.printf("%03d clients on %s:%d, %03d runs each. All times in µs.%n", NO_CLIENTS, hostname, port, NO_SAMPLES);
		range(0, NO_CLIENTS).unordered().parallel()
		.mapToObj(i -> CompletableFuture.supplyAsync(newClient(localAddress, groupClient), es).join())
		.map(array -> Arrays.stream(array).reduce(new DescriptiveStatistics(), Main::accumulate, Main::combine))
		.map(Main::toEvaluationString).forEach(System.out::println);

		es.shutdown();
		es.awaitTermination(5, TimeUnit.SECONDS);

		groupClient.shutdown();
		groupClient.awaitTermination(5, TimeUnit.SECONDS);

		server.close();
		groupServer.shutdown();
		groupServer.awaitTermination(5, TimeUnit.SECONDS);
	}

	private static DescriptiveStatistics accumulate(DescriptiveStatistics stats, Long value) {
		stats.addValue(value / 1000d);
		return stats;
	}

	private static DescriptiveStatistics combine(DescriptiveStatistics stats1, DescriptiveStatistics stats2) {
		Arrays.stream(stats2.getValues()).forEach(d -> stats1.addValue(d));
		stats2.clear();
		return stats1;
	}

	private static String toEvaluationString(DescriptiveStatistics stats) {
		String data = String.format("0.50 Percentile  = %8.2f, "
				+ "0.90 Percentile = %8.2f, " + "0.99 Percentile = %8.2f, "
				+ "min = %8.2f, " + "max = %8.2f", stats.getMean(),
				stats.getPercentile(90), stats.getPercentile(99),
				stats.getMin(), stats.getMax());
		stats.clear();
		return data;
	}
}