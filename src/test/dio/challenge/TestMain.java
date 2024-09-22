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
import java.util.HashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.*;

public class TestMain {

    private final ByteArrayOutputStream outContent =
        new ByteArrayOutputStream();
    private final PrintStream out = new PrintStream(outContent);
    private final PipedInputStream pipeIn = new PipedInputStream();
    private final PipedOutputStream pipeOut = new PipedOutputStream();
    private final PrintWriter writeToIn = new PrintWriter(pipeOut);
    private final IoAdapter ioAdapter = new StreamWrapper(out, pipeIn);

    @Before
    public void setup() throws IOException {
        pipeIn.connect(pipeOut);
    }

    @Test
    public void testEofAfterOpen() throws Exception{

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();

        presenter.mainMenu(repository);

        assertEquals("", "\nWellcome to ShellBank\n" +
"Login (1), NewAccount (2), Exit (0)\n" +
"Bye\n", outContent.toString());
    }

    @Test
    public void test0AfterOpen() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("0");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);

        assertEquals("", "\nWellcome to ShellBank\n" +
"Login (1), NewAccount (2), Exit (0)\n" +
"Bye\n", outContent.toString());
    }

    @Test
    public void test2AfterOpen() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("2");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);

        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        assertTrue("Expected output size to contain at least 4 lines",
                   outSplit.length >= 4);
        assertEquals("Expected line idx 3 to contain",
                     outSplit[3], "New Account:");
    }

    @Test
    public void testNewAccountJustName() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("2");
                writeToIn.println("Mbr");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);
        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        Optional<String> actualTypeYourName = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("New Account:"))
            .dropWhile(str -> !str.equals("Please type your name"))
            .findFirst();

        assertTrue("Expected output to contain e line equal to <Please type your name>",
                   actualTypeYourName.isPresent());

        Optional<String> actualDigitPassword = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("Please type your name"))
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Enter 4 digit number password"))
            .findFirst();

        assertTrue("Expected output to contain a line that contains <Choose a 4 digit number password>",
                   actualDigitPassword.isPresent());
    }

    @Test
    public void testNewAccountNonNumericPassword() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("2");
                writeToIn.println("Mbr");
                writeToIn.println("ABC");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);
        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        Optional<String> actualTypeYourName = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("New Account:"))
            .dropWhile(str -> !str.equals("Please type your name"))
            .findFirst();

        assertTrue("Expected output to contain e line equal to <Please type your name>",
                   actualTypeYourName.isPresent());

        Optional<String> actualDigitPassword = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("Please type your name"))
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Invalid password"))
            .findFirst();

        assertTrue("Expected output to contain a line that contains <Invalid password>",
                   actualDigitPassword.isPresent());
    }

    @Test
    public void testNewAccountIncorrectConfirmation() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("2");
                writeToIn.println("Mbr");
                writeToIn.println("1234");
                writeToIn.println("4321");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);
        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        Optional<String> actualTypeYourName = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("New Account:"))
            .dropWhile(str -> !str.equals("Please type your name"))
            .findFirst();

        assertTrue("Expected output to contain e line equal to <Please type your name>",
                   actualTypeYourName.isPresent());

        Optional<String> actualConfirmationPassword = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Enter 4 digit number password:"))
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Invalid confirmation"))
            .findFirst();

        assertTrue("Expected output to contain a line that contains <Invalid confirmation>",
                   actualConfirmationPassword.isPresent());
    }

    @Test
    public void testNewAccountCorrectConfirmation() throws Exception {

        final Repository repository = new RepositoryInMemory(new HashMap<>());
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("2");
                writeToIn.println("Mbr");
                writeToIn.println("1234");
                writeToIn.println("1234");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);
        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        Optional<String> actualTypeYourName = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("New Account:"))
            .dropWhile(str -> !str.equals("Please type your name"))
            .findFirst();

        assertTrue("Expected output to contain e line equal to <Please type your name>",
                   actualTypeYourName.isPresent());

        Optional<String> actualConfirmationPassword = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Enter 4 digit number password:"))
            .dropWhile(str -> !str.equals("Password will not be hidden while typing it"))
            .dropWhile(str -> !str.contains("Account created"))
            .findFirst();

        assertTrue("Expected output to contain a line that contains <Account created>",
                   actualConfirmationPassword.isPresent());
    }

    @Test
    public void testLogin() throws Exception {

        final HashMap<Integer, Account> repoBack = new HashMap<>();
        repoBack.put(1, new CheckingAccount("Mbr", "1234"));
        final Repository repository = new RepositoryInMemory(repoBack);
        final Presenter presenter =
            Main.defaultPresenter(ioAdapter, repository);

        Thread feedIn = new Thread(() -> {
                writeToIn.println("1");
                writeToIn.println("1");
                writeToIn.println("1234");
                writeToIn.flush();
                writeToIn.close();
        });

        feedIn.start();
        feedIn.join();
        presenter.mainMenu(repository);
        String output = outContent.toString();
        String[] outSplit = output.split("\n");

        Optional<String> actualTypeYourName = Arrays.stream(outSplit)
            .dropWhile(str -> !str.equals("Login:"))
            .dropWhile(str -> !str.contains("Number Account:"))
            .dropWhile(str -> !str.contains("Welcome Mbr"))
            .findFirst();

        assertTrue("Expected output to contain e line that contains <Welcome Mbr>",
                   actualTypeYourName.isPresent());
    }
}
