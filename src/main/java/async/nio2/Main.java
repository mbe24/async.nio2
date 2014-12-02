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
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

		AsynchronousChannelGroup groupServer = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));
		AsynchronousChannelGroup groupClient = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));

		InetSocketAddress isa = new InetSocketAddress("localhost", PORT);
		Server.newInstance(isa, groupServer);

		List<Client> clients = IntStream.range(0, NO_CLIENTS).mapToObj(i -> newClient(isa, groupClient)).collect(toList());
		
		final ExecutorService es = Executors.newFixedThreadPool(2);
		final List<Future<Long[]>> futures = clients.stream().map(client -> es.submit(client)).collect(toList());

		System.out.printf("%03d clients on localhost:%d, %03d runs each. All times in µs.%n", NO_CLIENTS, PORT, NO_SAMPLES);
		final DescriptiveStatistics stats = new DescriptiveStatistics();
		futures.forEach(f -> {
			try {
				Arrays.asList(Arrays.asList(f.get())).forEach(
						list -> {
							list.stream().forEach(l -> stats.addValue(l / 1000d));
							System.out.printf("0.50 Percentile  = %8.2f, "
									+ "0.90 Percentile = %8.2f, "
									+ "0.99 Percentile = %8.2f, "
									+ "min = %8.2f, " + "max = %8.2f%n",
									stats.getMean(), stats.getPercentile(90),
									stats.getPercentile(99), stats.getMin(),
									stats.getMax());
							stats.clear();
						});
			} catch (ExecutionException | InterruptedException e) {
				e.printStackTrace();
			}
		});

		groupClient.shutdownNow();
		groupClient.awaitTermination(1, TimeUnit.SECONDS);

		groupServer.shutdownNow();
		groupServer.awaitTermination(1, TimeUnit.SECONDS);

		es.shutdown();
	}
}