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
package at.ac.tuwien.infosys.jcloudscale.test.integration.base;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.UUID;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.util.ReflectionUtils;

import at.ac.tuwien.infosys.jcloudscale.management.CloudManager;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.FieldCloudObject;
import at.ac.tuwien.infosys.jcloudscale.test.testobject.FieldCloudObjectParent;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class TestFieldHandling 
{
	protected static CloudManager cs = null;
	 
	@Test
	public void testPublicFieldAccess() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.publicField = "public_changed";
		assertEquals(ob.publicField, "public_changed");
		assertEquals(ob.getPublicField(), "public_changed");
		assertEquals(ob.getPublicField(), ob.publicField);
	}
	
	@Test
	public void testPublicFieldAccessWithReference() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.publicRefField = new Date(1000);
		assertEquals(ob.publicRefField.toString(), new Date(1000).toString());
		assertEquals(ob.getPublicRefField().toString(), new Date(1000).toString());
		assertEquals(ob.publicRefField.toString(), ob.getPublicRefField().toString());
	}
	
	@Test
	public void testPublicFieldSetInCloud() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.setPublicField("remote");
		assertEquals(ob.publicField, "remote");
		assertEquals(ob.getPublicField(), "remote");
		assertEquals(ob.getPublicField(), ob.publicField);
		
	}
	
	@Test
	public void testPublicFieldSetInCloudWithRef() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.setPublicRefField(new Date(1000));
		assertEquals(ob.publicRefField.toString(), new Date(1000).toString());
		assertEquals(ob.getPublicRefField().toString(), new Date(1000).toString());
		assertEquals(ob.getPublicRefField().toString(), ob.publicRefField.toString());
		
	}
	
	@Test
	public void testPrivateFieldAccess() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		FieldCloudObject.setPrivateField(ob, "private_changed");
		assertEquals(ob.getPrivateField(), "private_changed");
		
	}
	
	@Test
	public void testProtectedFieldAccess() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		FieldCloudObject.setProtectedField(ob, "protected_changed");
		assertEquals(ob.getProtectedField(), "protected_changed");
		
	}
	
	@Test
	public void testPublicFieldSetViaReflection() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		// we need to go to the superclass as this is in fact a dynamic proxy generated
		// by CGLib
		Field field = ob.getClass().getSuperclass().getDeclaredField("publicField");
		field.set(ob, "public_changed");
		
		assertEquals(ob.publicField, "public_changed");
		assertEquals(ob.getPublicField(), "public_changed");
		assertEquals(ob.getPublicField(), ob.publicField);
		
	}
	
	@Test
	public void testPrivateFieldSetViaReflection() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		// we need to go to the superclass as this is in fact a dynamic proxy generated
		// by CGLib
		Field field = ob.getClass().getSuperclass().getDeclaredField("privateField");
		ReflectionUtils.makeAccessible(field);
		field.set(ob, "private_changed");
		
		assertEquals(ob.getPrivateField(), "private_changed");
		
	}
	
	@Test
	public void testPublicFieldGetViaReflection() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.setPublicField("public_changed");
		
		// we need to go to the superclass as this is in fact a dynamic proxy generated
		// by CGLib
		Field field = ob.getClass().getSuperclass().getDeclaredField("publicField");
		String val = (String) field.get(ob);
		
		assertEquals(val, "public_changed");
		
	}
	
	@Test
	public void testPrivateFieldGetViaReflection() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.setPrivateField("private_changed");
		
		// we need to go to the superclass as this is in fact a dynamic proxy generated
		// by CGLib
		Field field = ob.getClass().getSuperclass().getDeclaredField("privateField");
		ReflectionUtils.makeAccessible(field);
		String val = (String) field.get(ob);
		
		assertEquals(val, "private_changed");
		
	}
	
	@Test
	public void testPassByValue() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		UUID id = UUID.randomUUID();
		ob.serializable = id;
		assertEquals(ob.serializable, id);
		assertEquals(ob.getSerializable(), id);
		assertEquals(ob.getSerializable(), ob.serializable);
		
	}
	
	@Test
	public void testPublicFieldOfParentAccess() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.publicParentField = "public_parent_changed";
		assertEquals(ob.publicParentField, "public_parent_changed");
		assertEquals(ob.getPublicParentField(), "public_parent_changed");
		assertEquals(ob.getPublicParentField(), ob.publicParentField);
	}
	
	@Test
	public void testPublicParentFieldSetInCloud() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		ob.setPublicParentField("remote_parent");
		assertEquals(ob.publicParentField, "remote_parent");
		assertEquals(ob.getPublicParentField(), "remote_parent");
		assertEquals(ob.getPublicParentField(), ob.publicParentField);
		
	}
	
	@Test
	public void testProtectedFieldOfParentAccess() throws Exception {
		
		assertEquals(0,cs.countCloudObjects());
		FieldCloudObject ob = new FieldCloudObject();
		assertEquals(1,cs.countCloudObjects());
		
		FieldCloudObjectParent.setProtectedField(ob, "protected_parent_changed");
		assertEquals(ob.getProtectedParentField(), "protected_parent_changed");
	}
	
}
