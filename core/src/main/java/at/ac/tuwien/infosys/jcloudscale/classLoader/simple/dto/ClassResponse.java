/*
   Copyright 2013 Philipp Leitner

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package at.ac.tuwien.infosys.jcloudscale.classLoader.simple.dto;

import at.ac.tuwien.infosys.jcloudscale.messaging.objects.MessageObject;

public class ClassResponse extends MessageObject 
{
	private static final long serialVersionUID = -4347146882571719578L;

	public byte[] bytecode;
	
	public ClassResponse(byte[] bytecode)
	{
		this.bytecode = bytecode;
	}
}
