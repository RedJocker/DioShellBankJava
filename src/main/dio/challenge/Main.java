package dio.challenge;

import java.io.Console;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.function.Consumer;
import java.lang.IllegalStateException;
import org.mindrot.jbcrypt.BCrypt;


interface Repository {
    public Optional<Account> getAccountByNumber(int accountNumber);
    public boolean saveAccount(Account account);
}

class RepositoryInMemory implements Repository {
    final private Map<Integer, Account> database;

    public RepositoryInMemory(Map<Integer, Account> database){
        this.database = database;
    }

    @Override
    public Optional<Account> getAccountByNumber(int accountNumber) {
        return Optional.ofNullable(database.get(accountNumber));
    }

    @Override
    public boolean saveAccount(Account account) {

        final Account oldAccount =
            database.putIfAbsent(account.getNumber(), account);
        return oldAccount == null;
    }
}

class NumberSequenceGenerator {
    int number;

    NumberSequenceGenerator(int base) {
        number = base;
    }
    public int newNumber() {
        return ++number;
    }
}

interface Account {
    static NumberSequenceGenerator number = new NumberSequenceGenerator(0);

    public int getNumber();
    public String getBranch();
    public String getUserName();
    public double getBalance();
    public boolean verifyPass(String passAttempt);

    public static int newNumber() {
        return number.newNumber();
    }
}

class CheckingAccount implements Account {

    private final int number;
    private final String branch;
    private final String username;
    private final double balance;
    private final String hashpass;

    public int getNumber() {
        return this.number;
    }
    public String getBranch() {
        return this.branch;
    }
    public String getUserName() {
        return this.username;
    }
    public double getBalance() {
        return this.balance;
    }

    public boolean verifyPass(String passAttempt) {
	return BCrypt.checkpw(passAttempt, hashpass);
    }

    private CheckingAccount(
        int number,
        String branch,
        String username,
        double balance,
        String plainPass) {

        this.number = number;
        this.branch = branch;
        this.username = username;
        this.balance = balance;
        this.hashpass = BCrypt.hashpw(plainPass, BCrypt.gensalt());
    }

    public CheckingAccount(String username, String pass) {
        this(Account.newNumber(), "4242-x", username, 0.0, pass);
    }

    @Override
    public String toString() {
        return String.format(
            "CheckingAccount(" +
            "number: %d, branch: %s, username: %s, balance: %.2f)",
            number, branch, username, balance);
    }
}

interface IoAdapter {
    public IoAdapter printf(String format, Object ... args);
    public String readLine(String fmt, Object ... args);
    public String readLine();
    public String readPassword(String fmt, Object ... args);
    public String readPassword();

    default int readNumberUnsigned() {
	String inputLine;
        int option;

        inputLine = this.readLine();
	if (inputLine == null)
	    return 0;
        try {
            option = Integer.parseInt(inputLine);
            if (option < 0) {
                option = -1;
            }
        } catch (NumberFormatException ex) {
            option = -1;
        }

        return option;
    }
}

class ConsoleWrapper implements IoAdapter {

    Console console;

    public ConsoleWrapper(Console console) {
	this.console = console;
    }

    @Override
    public IoAdapter printf(String format, Object ... args) {
	console.printf(format, args);
	return this;
    }
    @Override
    public String readLine(String fmt, Object ... args) {
	return console.readLine(fmt, args);
    }
    @Override
    public String readLine() {
	return console.readLine();
    }
    @Override
    public String readPassword(String fmt, Object ... args) {
	char[] arr = console.readPassword(fmt, args);
	if (arr == null)
	    return null;
	return String.valueOf(arr);
    }
    @Override
    public String readPassword() {
	char[] arr = console.readPassword();
	if (arr == null)
	    return null;
	return String.valueOf(arr);
    }
}

class StreamWrapper implements IoAdapter {
    PrintStream out;
    Scanner in;

    public StreamWrapper(PrintStream out, InputStream in) {
	this.out = out;
	this.in = new Scanner(in);
    }

    @Override
    public IoAdapter printf(String format, Object ... args) {
	out.printf(format, args);
	return this;
    }
    @Override
    public String readLine(String fmt, Object ... args) {
	out.printf(fmt, args);
	return this.readLine();
    }
    @Override
    public String readLine() {
	return in.hasNext() ? in.next() : null;
    }
    @Override
    public String readPassword(String fmt, Object ... args) {
	out.printf("Warning: this is not a fully functional terminal\n");
	out.printf("Password will not be hidden while typing it\n");
	out.printf(fmt, args);
	return this.readPassword();
    }
    @Override
    public String readPassword() {
	return this.readLine();
    }
}

abstract class Menu<A> {
    protected static final String invalidChoice = "Invalid Choice\n";
    protected final IoAdapter console;

    protected Menu(IoAdapter console) {
	this.console = console;
    }

    abstract String getMenuString();

    abstract Integer getMenuSize();

    abstract void loop(Repository repository, A args);

    protected int promptMenuChoice() {
	int option;

	while (true) {
	    console.printf(this.getMenuString());
	    option = console.readNumberUnsigned();
	    if (option < 0 || option > this.getMenuSize()) {
		console.printf(this.invalidChoice);
		continue ;
	    } else {
		return option;
	    }
	}
    }
}

class MainMenu extends Menu<Void> {

    private static final String wellcome = "\nWellcome to ShellBank\n";
    private static final String startMenu = "Login (1), NewAccount (2), Exit (0)\n";
    private static final String bye = "Bye\n";

    private final NewAccountIoForm newAccountForm;
    private final LoginIoForm loginForm;
    private final UserMenu userMenu;

    MainMenu(IoAdapter console,
	     NewAccountIoForm newAccountForm,
	     LoginIoForm loginForm,
	     UserMenu userMenu) {
	super(console);
	this.newAccountForm = newAccountForm;
	this.loginForm = loginForm;
	this.userMenu = userMenu;
    }

    public void displayWellcome() {
	console.printf(this.wellcome);
    }

    public void displayBye() {
	console.printf(this.bye);
    }

    public void userMenu(Account account, Repository repository) {
	userMenu.loop(repository, account);
    }

    private void promptNewAccount(Repository repository) {
        boolean wasCreated = false;
        Account account = null;

	account = newAccountForm.collect(null);
	if (account != null)
	    wasCreated = repository.saveAccount(account);
	else
	    wasCreated = false;

        if (!wasCreated && account == null) {
            console.printf("Account creation failed\n");
        }
        else {
            console.printf("Account created: %s\n", account);
        }
    }

    private void login(Repository repository) {

	final Optional<Account> maybeAccount =
	    loginForm.collect(null);

	if (!maybeAccount.isPresent()) {
	    return ;
        }
	final Account account = maybeAccount.get();
	this.userMenu(account, repository);
    }
    
    @Override
    public Integer getMenuSize() {
	return 2;
    }

    @Override
    public String getMenuString() {
	return this.startMenu;
    }

    @Override
    public void loop(Repository repository, Void args) {

	int choice = -1;
	while (choice != 0) {
	    choice = this.promptMenuChoice();
	    if (choice == 1) {
		this.login(repository);
	    } else if (choice == 2) {
		this.promptNewAccount(repository);
	    }
	}
    }
}

class UserMenu extends Menu<Account> {

    private static final String startMenu = "Balance (1), Loan (2), Back (0)\n";
    private Optional<Account> maybeUser = Optional.empty();
    
    UserMenu(IoAdapter console) {
	super(console);
    }

    public void startSessionFor(Account account) {
	maybeUser = Optional.of(account);
    }

    private void logOffSession() {
	maybeUser = Optional.empty();
    }

    private void logInSession(Account account) {
	maybeUser = Optional.ofNullable(account);
    }

    private void updateSession(Account updated) {
	if (!maybeUser.isPresent())
	    return ;
	final Account current = maybeUser.get();
	if (current.number != updated.number) {
	    throw new IllegalStateException(
                "updateSession shold be called only whith updated copy of current Account");
	} else
	    this.maybeUser = Optional.of(updated);
    }

    private void greeting(Account account) {
	console.printf("Welcome to ShellBank %s\n", account.getUserName());
    }

    private void balance(Account account) {
	console.printf("balance: %.2f\n", account.getBalance());
    }

    @Override
    public Integer getMenuSize() {
	return 2;
    }
    
    @Override
    public String getMenuString() {
	return this.startMenu;
    }

    @Override
    public void loop(Repository repository, Account account) {

	int choice = -1;
	
	this.logInSession(account);
	this.greeting(account);
	account = null;
	while (choice != 0 && maybeUser.isPresent()) {
	    final Account current = maybeUser.get(); 
	    choice = this.promptMenuChoice();
	    if (choice == 1) {
		balance(current); 
	    } else if (choice == 2) {
		console.printf("choice 2\n"); 
	    }
	}
	this.logOffSession();
    }
}

interface Form<R, A> {
    R collect(A args);
}

abstract class IoForm<R, A> implements Form<R, A>{
    protected final IoAdapter console;

    protected IoForm(IoAdapter console) {
	this.console = console;
    }

    protected boolean tryAgain() {
	final String tryAgainStr =
	    console.readLine("Quit (0) TryAgain (Any)\n");
	return tryAgainStr != null && !tryAgainStr.equals("0");
    }
}

class NewAccountIoForm extends IoForm<Account, Void> {
    private static final String promptName = "Please type your name\n";

    public NewAccountIoForm(IoAdapter console) {
	super(console);
    }

    @Override
    public Account collect(Void args) {
        Account account = null;

	console.printf("New Account:\n");
	while (true){
	    final String name = console.readLine(this.promptName);
	    if (name == null || name.isBlank()) {
		console.printf("Invalid name\n");
		if (tryAgain())
		    continue;
		else
		    break;
	    }
	    final String pass =
		console.readPassword("Enter 4 digit number password: ");
	    if (pass == null || !pass.matches("\\d{4}")) {
		console.printf("Invalid password\n");
		if (tryAgain())
		    continue;
		else
		    break;
	    }
	    final String confirm =
		console.readPassword("Enter confirm password: ");
	    if (confirm == null || !confirm.equals(pass)) {
		console.printf("Invalid confirmation\n");
		if (tryAgain())
		    continue;
		else
		    break;
	    }
	    account = new CheckingAccount(name, pass);
	    break ;
	}
	return account;
    }
}

class LoginIoForm extends IoForm<Optional<Account>, Void> {
    final Repository repository;

    public LoginIoForm(IoAdapter console, Repository repository) {
	super(console);
	this.repository = repository;
    }

    @Override
    public Optional<Account> collect(Void args) {
        int numberAccount;
        Optional<Account> maybeAccount;
	Account account;
        String pass;

        console.printf("Login:\n");
	console.printf("Number Account: ");

	numberAccount = console.readNumberUnsigned();
	if (numberAccount < 0) {
            console.printf("Account not found\n");
	    return Optional.empty();
        }

	maybeAccount = repository.getAccountByNumber(numberAccount);
	if (!maybeAccount.isPresent()) {
            console.printf("Account not found\n");
	    return Optional.empty();
        }
	account = maybeAccount.get();

	pass = console.readPassword("Enter 4 digit number password: ");
        if (!account.verifyPass(pass)) {
            console.printf("Invalid password\n");
	    return Optional.empty();
	}
	return maybeAccount;
    }
}

class Presenter {

    private final IoAdapter console;
    private final MainMenu mainMenu;
    
    public Presenter(
	    IoAdapter console,
	    MainMenu mainMenu) {
        this.console = console;
	this.mainMenu = mainMenu;
    }

    public void mainMenu(Repository repository) {
	mainMenu.displayWellcome();
	mainMenu.loop(repository, null);
	mainMenu.displayBye();
    }
}

class Main {

    static public Presenter defaultPresenter(
	    IoAdapter ioAdapter, Repository repository) {

	final NewAccountIoForm newAccountForm = new NewAccountIoForm(ioAdapter);
	final LoginIoForm loginForm = new LoginIoForm(ioAdapter, repository);
	final UserMenu userMenu = new UserMenu(ioAdapter); 

	final MainMenu mainMenu = new MainMenu(
            ioAdapter,
            newAccountForm,
	    loginForm,
	    userMenu
	);
	final Presenter presenter = new Presenter(
	    ioAdapter,
	    mainMenu
	);
	return presenter;
    }

    public static void main(String[] args) {

	final Console console = System.console();
	final IoAdapter ioAdapter;
	if (console != null)
	    ioAdapter = new ConsoleWrapper(console);
	else {
	    ioAdapter = new StreamWrapper(System.out, System.in);
	}

	final Repository repository = new RepositoryInMemory(new HashMap<>());
	final Presenter presenter = defaultPresenter(ioAdapter, repository);

        presenter.mainMenu(repository);
    }
}
