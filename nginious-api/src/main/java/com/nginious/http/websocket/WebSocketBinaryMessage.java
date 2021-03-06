/**
 * Copyright 2012 NetDigital Sweden AB
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
*/

package com.nginious.http.websocket;

/**
 * A binary web socket message has an opcode {@link Opcode#BINARY} and carries
 * a binary payload.
 * 
 * @author Bojan Pisler, NetDigital Sweden AB
 *
 */
public interface WebSocketBinaryMessage extends WebSocketMessage {
	
	/**
	 * Returns the binary paylod of this web socket binary message.
	 * 
	 * @return the payload
	 */
	public byte[] getMessage();
	
	/**
	 * Returns the payload length of this web socket binary message.
	 * 
	 * @return the payload length
	 */
	public long length();
}
