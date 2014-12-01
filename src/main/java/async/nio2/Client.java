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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static async.nio2.Main.NO_SAMPLES;

public class Client implements Callable<Long[]>, AutoCloseable {

	private final Random r = new Random();
	private static final int MAX = Integer.MAX_VALUE / 2;

	private final AsynchronousSocketChannel asc;

	private Client(AsynchronousSocketChannel asc) {
		this.asc = asc;
	}

	public static Client newClient(InetSocketAddress isa, AsynchronousChannelGroup group) throws IOException {
		AsynchronousSocketChannel asc = AsynchronousSocketChannel.open(group);
		try {
			asc.connect(isa).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		return new Client(asc);
	}

	@Override
	public Long[] call() throws Exception {
		Long[] runs = new Long[NO_SAMPLES];

		int[] numbers = new int[NO_SAMPLES];
		for (int i = 0; i < NO_SAMPLES; i++)
			numbers[i] = r.nextInt(MAX);

		ByteBuffer bb = ByteBuffer.allocate(4);
		for (int i = 0; i < NO_SAMPLES; i++) {
			bb.putInt(numbers[i]);
			bb.flip();

			long start = System.nanoTime();
			asc.write(bb).get();
			bb.flip();

			asc.read(bb).get();
			long duration = System.nanoTime() - start;
			bb.flip();

			int doubled = bb.getInt();
			bb.flip();

			if (doubled / 2 == numbers[i])
				runs[i] = duration;
			else
				throw new IllegalArgumentException("Server computation fault!");

		}

		close();
		return runs;
	}

	@Override
	public void close() throws IOException {
		asc.close();
	}
}