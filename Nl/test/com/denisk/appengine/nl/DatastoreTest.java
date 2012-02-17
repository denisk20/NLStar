package com.denisk.appengine.nl;

import static org.junit.Assert.assertEquals;
import static com.google.appengine.api.datastore.FetchOptions.Builder.withLimit;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.denisk.appengine.nl.server.DataHandler;
import com.denisk.appengine.nl.server.data.Category;
import com.denisk.appengine.nl.server.data.Good;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public class DatastoreTest {
	 private final LocalServiceTestHelper helper =
		        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

	@Before
	public void before() {
		helper.setUp();
	}
	
	@After
	public void after() {
		helper.tearDown();
	}
	
	@Test
	public void datastoreTest() {
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		final String kind = "yum";
		assertEquals(0, ds.prepare(new Query(kind)).countEntities(withLimit(10)));
		ds.put(new Entity(kind));
		ds.put(new Entity(kind));
		assertEquals(2, ds.prepare(new Query(kind)).countEntities(withLimit(10)));
	}

	@Test
	public void dataHandlerBasicTest() throws EntityNotFoundException {
		DataHandler dh = new DataHandler();
		
		Category c = new Category();
		c.setName("hello");
		c.setDescription("desc");
		
		Good g1 = new Good();
		Good g2 = new Good();

		g1.setName("g1_name");
		g2.setName("g2_name");
		
		g1.setName("g1_desc");
		g2.setDescription("g2_desc");
		
		c.getGoods().add(g1);
		c.getGoods().add(g2);
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		assertEquals(0, ds.prepare(new Query(Category.KIND)).countEntities(withLimit(10)));
		assertEquals(0, ds.prepare(new Query(Good.KIND)).countEntities(withLimit(10)));

		Key key = dh.saveCategoryWithGoods(c);
		
		assertEquals(1, ds.prepare(new Query(Category.KIND)).countEntities(withLimit(10)));
		assertEquals(2, ds.prepare(new Query(Good.KIND)).countEntities(withLimit(10)));
		
		ds.get(key);
	}
}