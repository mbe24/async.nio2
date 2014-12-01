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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

import async.nio2.ChannelState;

public class ReadHandler implements CompletionHandler<Integer, ChannelState> {

	private final WriteHandler writeHandler = new WriteHandler();

	@Override
	public void completed(Integer result, ChannelState attachment) {
		if (result == -1) {
			try {
				attachment.channel().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		ByteBuffer rb = attachment.readBuffer();
		if (rb.hasRemaining())
			attachment.channel().read(attachment.readBuffer(), attachment, this);

		rb.flip();

		int receivedNo = rb.getInt();
		rb.flip();

		ByteBuffer wb = attachment.writeBuffer();
		wb.clear();
		wb.putInt(receivedNo * 2);
		wb.flip();

		// write answer
		attachment.channel().write(wb, attachment, writeHandler);

		// read next
		rb.clear();
		attachment.channel().read(attachment.readBuffer(), attachment, this);
	}

	@Override
	public void failed(Throwable exc, ChannelState attachment) {
		System.out.printf("Error while reading from client #%02d!%n", attachment.getInstance());
	}
}