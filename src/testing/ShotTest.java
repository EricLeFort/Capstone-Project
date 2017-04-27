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

	@Test
	public void testAlterScore(){
		Shot a = new Shot(1, 0.5, 1, 1);
		assertEquals(-4, a.getScore());
		
		a.alterScore(4);
		assertEquals(0, a.getScore());
		
		a.alterScore(505);
		assertEquals(505, a.getScore());
		
		a.alterScore(-504);
		assertEquals(1, a.getScore());
	}//testAlterScore()
	
	@Test
	public void testSetXPosition(){
		Shot a = new Shot(1, 0.5, 2, 1);
		a.setXPosition(0.2);
		assertEquals(0.2, a.getXPosition(), DELTA);
		
		try{									//Large X value
			a.setXPosition(1.849);
		}catch(IllegalArgumentException iae){
			assertTrue(a.getXPosition() == 0.2);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{									//Small X value
			a.setXPosition(-0.001);
		}catch(IllegalArgumentException iae){
			assertTrue(a.getXPosition() == 0.2);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
	}//testSetXPosition()
	
	@Test
	public void testSetYPosition(){
		Shot a = new Shot(1, 0.5, 2, 1);
		a.setYPosition(0.2);
		assertEquals(0.2, a.getYPosition(), DELTA);
		
		try{									//Large Y value
			a.setYPosition(0.922);
		}catch(IllegalArgumentException iae){
			assertTrue(a.getYPosition() == 0.2);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
		
		try{									//Small Y value
			a.setYPosition(-0.001);
		}catch(IllegalArgumentException iae){
			assertTrue(a.getYPosition() == 0.2);
		}catch(Exception e){
			fail("Unexpected exception.");
		}
	}//testSetYPosition()
	
}//ShotTest
