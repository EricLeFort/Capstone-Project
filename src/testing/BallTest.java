package testing;

import static org.junit.Assert.*;

import org.junit.Test;
import pcController.Ball;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class BallTest{
	private static final double DELTA = 1e-15;
	
	@Test
	public void testConstructor(){
		Ball a = new Ball(1.0, 0.7, (byte)0);		//Good inputs
		
		assertEquals(1, a.getXPosition(), DELTA);
		assertEquals(0.7, a.getYPosition(), DELTA);
		assertEquals(0, a.getValue());
		
		a = null;
		try{										//Large X
			new Ball(1.87658, 0.7, (byte)0);
			fail("Expected exception for large x value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (1.87658, 0.7, 0)");
		}
		
		try{										//Large Y
			new Ball(1, 0.94958, (byte)0);
			fail("Expected exception for large y value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (1, 0.94958, 0)");
		}
		
		try{										//Small X
			new Ball(-1.001, 0.7, (byte)0);
			fail("Expected exception for small x value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (-0.001, 0.7, 0)");
		}
		
		try{										//Small Y
			new Ball(1, -1.001, (byte)0);
			fail("Expected exception for small y value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (1, 0.001, 0)");
		}
		
		try{										//Large ball value
			new Ball(1, 0.7, (byte)16);
			fail("Expected exception for large ball value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (1, 0.7, 16)");
		}
		
		try{										//Small ball value
			new Ball(1, 0.7, (byte)-1);
			fail("Expected exception for small ball value not thrown.");
		}catch(IllegalArgumentException iae){
			assertEquals(null, a);
		}catch(Exception e){
			fail("Incorrect exception thrown for constructor (1, 0.7, -1)");
		}
	}//testConstructor

	@Test
	public void testAlterX(){
		Ball a = new Ball(1, 0.5, (byte)2);
		assertEquals(1, a.getXPosition(), DELTA);
		
		a.alterX(1);
		assertEquals(2, a.getXPosition(), DELTA);
		
		a.alterX(505.23);
		assertEquals(507.23, a.getXPosition(), DELTA);
		
		a.alterX(-504.23);
		assertEquals(3, a.getXPosition(), DELTA);
	}//testAlterX()
	
	@Test
	public void testAlterY(){
		Ball a = new Ball(1, 0.5, (byte)2);
		assertEquals(0.5, a.getYPosition(), DELTA);
		
		a.alterY(1);
		assertEquals(1.5, a.getYPosition(), DELTA);
		
		a.alterY(505.23);
		assertEquals(506.73, a.getYPosition(), DELTA);
		
		a.alterY(-504.23);
		assertEquals(2.5, a.getYPosition(), DELTA);
	}//testAlterY()
	
}//BallTest