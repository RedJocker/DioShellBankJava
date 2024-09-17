package dio.challenge;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.IOException;

public class TestMain {

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PipedInputStream pipeIn = new PipedInputStream();
    private final PipedOutputStream pipeOut = new PipedOutputStream();
    private final PrintWriter out = new PrintWriter(pipeOut);
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;
    
    @Before
    public void setUpStreams() throws IOException {
	pipeIn.connect(pipeOut);
	System.setOut(new PrintStream(outContent));
	System.setErr(new PrintStream(errContent));
	System.setIn(pipeIn);
    }
    
    @After
    public void restoreStreams() {
	System.setOut(originalOut);
	System.setErr(originalErr);
	System.setIn(originalIn);
    }
    
    @Test
    public void testMethodShouldReturnTrue() {
        
        int input = 5;
	
        assertEquals("Expected true for input 5", 5, input);
	String[] args = {};
	out.println("0");
	out.println("0");
	out.println("0");
	out.println("0");
	out.flush();
	out.close();
	Main.main(args);
	assertEquals("hello", outContent.toString());
    }
}
