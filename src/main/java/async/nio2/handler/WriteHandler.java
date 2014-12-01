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

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import async.nio2.ChannelState;

public class WriteHandler implements CompletionHandler<Integer, ChannelState> {

	@Override
	public void completed(Integer result, ChannelState attachment) {
		ByteBuffer wb = attachment.writeBuffer();

		AsynchronousSocketChannel channel = attachment.channel();
		if (wb.remaining() > 0) {
			channel.write(wb, attachment, this);
		} else {
			wb.flip();
		}
	}

	@Override
	public void failed(Throwable exc, ChannelState attachment) {
		System.out.printf("Error while writing to client #%02d!%n", attachment.getInstance());
	}
}