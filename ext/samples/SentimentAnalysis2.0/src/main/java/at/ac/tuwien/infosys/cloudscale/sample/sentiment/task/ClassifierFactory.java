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
package at.ac.tuwien.infosys.jcloudscale.sample.sentiment.task;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import classifier.ClassifierBuilder;
import classifier.IClassifier;
import classifier.WeightedMajority;
import classifier.WekaClassifier;

public class ClassifierFactory 
{
	
	private static volatile Classifier singletonInstance =null;
	private static final Object syncRoot = new Object();
	
	//-----------------------------------------------
	
	private ClassifierFactory(){}
	
	//-----------------------------------------------
	
	public static Classifier createClassifier() 
	{
		try
		{
			return new Classifier();
		}
		catch(Exception ex)
		{
			String canonicalPath = "";
			try {
				canonicalPath = (new File (".")).getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
			throw new RuntimeException("Failed to construct Classifier. Current Directory is " + canonicalPath, ex);
		}
	}
	
	public static Classifier getSingletonInstance()
	{
		if(singletonInstance == null)
			synchronized (syncRoot) 
			{
				if(singletonInstance == null)
					return singletonInstance = createClassifier();
			}
		
		return singletonInstance;
	}
}
