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
package async.nio2.handler;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import async.nio2.ChannelState;

public class AcceptHandler implements CompletionHandler<AsynchronousSocketChannel, ChannelState> {

	private final AsynchronousServerSocketChannel assc;
	private final ReadHandler readHandler = new ReadHandler();

	public AcceptHandler(AsynchronousServerSocketChannel assc) {
		this.assc = assc;
	}

	@Override
	public void completed(AsynchronousSocketChannel result, ChannelState attachment) {
		// accept next connection
		assc.accept(ChannelState.newInstance(), this);

		// handle this connection
		attachment.initChannel(result);
		result.read(attachment.readBuffer(), attachment, readHandler);
	}

	@Override
	public void failed(Throwable exc, ChannelState attachment) {
		System.out.println("Error while accepting client: " + exc.toString());
	}
}