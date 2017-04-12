package testing;

import static org.junit.Assert.*;

import org.junit.Test;
import pcController.Shot;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class ShotTest{
	private static final double DELTA = 1e-15;
	
	@Test
	public void testConstructor(){
		Shot a = new Shot(1.0, 0.5, 3.5, 1.0);				//Good inputs
		assertEquals(a.getXPosition(), 1.0, DELTA);
		assertEquals(a.getYPosition(), 0.5, DELTA);
		assertEquals(a.getAngle(), 3.5, DELTA);
		assertEquals(a.getPower(), 1.0, DELTA);
		
		a = null;
		try{												//Large X value
			a = new Shot(1.87658, 0.5, 3.5, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Small X value
			a = new Shot(-0.001, 0.5, 3.5, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Large Y value
			a = new Shot(1, 0.94958, 3.5, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Small Y value
			a = new Shot(1, -0.001, 3.5, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Large angle
			a = new Shot(1, 0.5, 6.284, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Small angle
			a = new Shot(1, 0.5, -0.01, 1);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Large power
			a = new Shot(1, 0.5, 3.5, 1.001);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{												//Small power
			a = new Shot(1, 0.5, 3.5, 0);
		}catch(IllegalArgumentException iae){
			assertTrue(a == null);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
	}//testConstructor()
	
}//ShotTest
