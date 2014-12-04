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
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;

import async.nio2.handler.AcceptHandler;

public class Server implements AutoCloseable {

	private final AsynchronousServerSocketChannel assc;

	private Server(AsynchronousServerSocketChannel assc) {
		this.assc = assc;
	}

	public static Server newInstance(InetSocketAddress isa, AsynchronousChannelGroup group) throws IOException {
		AsynchronousServerSocketChannel assc = AsynchronousServerSocketChannel.open(group).bind(isa);
		AcceptHandler handler = new AcceptHandler(assc);
		assc.accept(ChannelState.newInstance(), handler);
		return new Server(assc);
	}

	public InetSocketAddress getLocalAddress() throws IOException {
		return (InetSocketAddress) assc.getLocalAddress();
	}

	@Override
	public void close() throws IOException {
		assc.close();
	}
}