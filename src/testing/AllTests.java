package testing;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
	BallTest.class,
	EventHandlerTest.class,
	InferenceEngineTest.class,
	PCCommunicatorTest.class,
	PCVRTest.class,
	ShotTest.class,
	SimulationInstanceTest.class,
	TableStateTest.class
})

public class AllTests{ }//AllTests
