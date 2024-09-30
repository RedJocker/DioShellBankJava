package dio.challenge;

import java.io.Console;
import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;
import java.time.ZoneId;
import java.time.LocalDateTime;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStream;
import java.util.function.Consumer;
import java.lang.IllegalStateException;
import org.mindrot.jbcrypt.BCrypt;

class Pair<Fst, Snd> {
    private final Fst fst;
    private final Snd snd;

    Pair(Fst fst, Snd snd) {
	this.fst = fst;
	this.snd = snd;
    }

    public Fst getFst() {
	return this.fst;
    }

    public Snd getSnd() {
	return this.snd;
    }
}

interface Repository {
    public Optional<Account> getAccountByNumber(int accountNumber);
    public boolean saveAccount(Account account);
    public boolean update(Account account);
    public boolean update(Pair<Account, Account> accountPair);
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

    @Override
    public boolean update(Account account) {
	if (!database.containsKey(account.getNumber()))
	    return false;
	database.put(account.getNumber(), account);
	return true;
    }

    @Override
    public boolean update(Pair<Account, Account> accountPair) {
	if (!database.containsKey(accountPair.getFst().getNumber())
	    || !database.containsKey(accountPair.getSnd().getNumber()))
	    return false;
	
	database.put(accountPair.getFst().getNumber(), accountPair.getFst());
	database.put(accountPair.getSnd().getNumber(), accountPair.getSnd());
	return true;
	
    }
}

class Service {
    private final Repository repository;

    Service(Repository repository) {
	this.repository = repository;
    }

    public boolean createAccount(Account account) {
	if (account != null)
	    return repository.saveAccount(account);
	else
	    return false;
    }

    public Optional<Account> getAccountByNumber(int accountNumber) {
        return repository.getAccountByNumber(accountNumber);
    }

    public Optional<CheckingAccount> loan(
					  double amount,
					  CheckingAccount account) {
	CheckingAccount updated = account.loan(amount);
	boolean wasUpdated = repository.update(updated);
	if (wasUpdated) {
	    return Optional.of(updated);
	} else {
	    return Optional.empty();
	}
    }

    public Optional<Account> deposit(double amount, Account account) {
	Account updated = account.deposit(amount);
	boolean wasUpdated = repository.update(updated);
	if (wasUpdated) {
	    return Optional.of(updated);
	} else {
	    return Optional.empty();
	}
    }

    public Optional<Account> withdraw(double amount, Account account) {
	Account updated = account.withdraw(amount);
	boolean wasUpdated = repository.update(updated);
	if (wasUpdated) {
	    return Optional.of(updated);
	} else {
	    return Optional.empty();
	}
    }

    public Optional<Account> transfer(double amount, Account from, Account to) {
	Pair<Account, Account> updatedPair = from.transfer(amount, to);
	boolean wasUpdated = repository.update(updatedPair);
	if (wasUpdated) {
	    return Optional.of(updatedPair.getFst());
	} else {
	    return Optional.empty();
	}
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
    public Account deposit(double amount);
    public Account withdraw(double amount);
    public boolean isValidWithdraw(double amount);
    public Pair<Account, Account> transfer(double amount, Account to);
    Account copyWithAmount(double amount);

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
    private final double loanLimit;
    private final double loanCurrent;

    @Override
    public int getNumber() {
        return this.number;
    }
    @Override
    public String getBranch() {
        return this.branch;
    }
    @Override
    public String getUserName() {
        return this.username;
    }
    @Override
    public double getBalance() {
        return this.balance;
    }
    @Override
    public boolean verifyPass(String passAttempt) {
	return BCrypt.checkpw(passAttempt, hashpass);
    }

    @Override
    public Account copyWithAmount(double amount) {
	return new CheckingAccount(
				   this.number,
				   this.branch,
				   this.username,
				   amount,
				   this.hashpass,
				   this.loanLimit,
				   this.loanCurrent
				   );
    }

    @Override
    public Account deposit(double amount) {
	return this.copyWithAmount(this.balance + amount);
    }

    @Override
    public boolean isValidWithdraw(double amount) {
	return this.balance >= amount;
    }

    @Override
    public Account withdraw(double amount) {
	return this.copyWithAmount(this.balance - amount);
    }

    @Override
    public Pair<Account, Account> transfer(double amount, Account to) {
	Account updatedFrom = this.copyWithAmount(this.balance - amount);
	
	Account updatedTo = to.copyWithAmount(to.getBalance() + amount);
	return new Pair(updatedFrom, updatedTo);
    }

    public double getLoanLimit() {
	return this.loanLimit;
    }

    private CheckingAccount(
			    int number,
			    String branch,
			    String username,
			    double balance,
			    String hashPass,
			    double loanLimit,
			    double loanCurrent) {

        this.number = number;
        this.branch = branch;
        this.username = username;
        this.balance = balance;
        this.hashpass = hashPass;
	this.loanLimit = loanLimit;
	this.loanCurrent = loanCurrent;
    }

    public CheckingAccount(String username, String pass) {
        this(Account.newNumber(), "4242-x", username, 0.0,
	     BCrypt.hashpw(pass, BCrypt.gensalt()), 500.0, 0.0);
    }

    public boolean isValidLoanRequest(Optional<Double> maybeLoanRequest) {
	if (!maybeLoanRequest.isPresent())
	    return false;
	double loanRequest = maybeLoanRequest.get();
	return loanRequest <= this.loanLimit && loanRequest >= 1.0;
    }

    public CheckingAccount loan(double loanAmount) {
	return new CheckingAccount(
				   this.number,
				   this.branch,
				   this.username,
				   this.balance + loanAmount,
				   this.hashpass,
				   this.loanLimit - loanAmount,
				   this.loanCurrent + loanAmount
				   );
    }

    @Override
    public String toString() {
        return String.format(
			     "CheckingAccount(" +
			     "number: %d, branch: %s, " +
			     "username: %s, balance: %.2f)",
			     number, branch, username, balance);
    }
}

class SavingAccount implements Account {

    private final int number;
    private final String branch;
    private final String username;
    private final double balance;
    private final String hashpass;

    @Override
    public int getNumber() {
        return this.number;
    }
    @Override
    public String getBranch() {
        return this.branch;
    }
    @Override
    public String getUserName() {
        return this.username;
    }
    @Override
    public double getBalance() {
        return this.balance;
    }
    @Override
    public boolean verifyPass(String passAttempt) {
	return BCrypt.checkpw(passAttempt, hashpass);
    }

    @Override
    public Account copyWithAmount(double amount) {
	return new SavingAccount(
				 this.number,
				 this.branch,
				 this.username,
				 amount,
				 this.hashpass
				 );
    }

    @Override
    public Account deposit(double amount) {
	return this.copyWithAmount(this.balance + amount);
    }

    @Override
    public boolean isValidWithdraw(double amount) {
	return this.balance >= amount;
    }

    @Override
    public Account withdraw(double amount) {
	return this.copyWithAmount(this.balance - amount);
    }

    @Override
    public Pair<Account, Account> transfer(double amount, Account to) {
	Account updatedFrom = this.copyWithAmount(this.balance - amount);
	
	Account updatedTo = to.copyWithAmount(to.getBalance() + amount);
	return new Pair(updatedFrom, updatedTo);
    }

    private SavingAccount(
			  int number,
			  String branch,
			  String username,
			  double balance,
			  String hashPass) {
        this.number = number;
        this.branch = branch;
        this.username = username;
        this.balance = balance;
        this.hashpass = hashPass;
    }

    public SavingAccount(String username, String pass) {
        this(Account.newNumber(), "4242-x", username, 0.0,
	     BCrypt.hashpw(pass, BCrypt.gensalt()));
    }

    @Override
    public String toString() {
        return String.format(
			     "SavingAccount(" +
			     "number: %d, branch: %s, " +
			     "username: %s, balance: %.2f)",
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

    default Optional<Double> readDoublerUnsigned() {
	String inputLine;
        Optional<Double> maybeNumber;

        inputLine = this.readLine();
	if (inputLine == null)
	    return Optional.empty();
        try {
	    double number = Double.parseDouble(inputLine);
            if (number < 0) {
                maybeNumber = Optional.empty();
            } else {
		maybeNumber = Optional.of(number);
	    }
        } catch (NumberFormatException ex) {
            maybeNumber = Optional.empty();
        }
        return maybeNumber;
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

    abstract void loop(Service service, A args);

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

    private static final String welcome = "\nWelcome to ShellBank\n";
    private static final String startMenu =
	"Login (1), NewAccount (2), Exit (0)\n";
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

    public void displayWelcome() {
	console.printf(this.welcome);
    }

    public void displayBye() {
	console.printf(this.bye);
    }

    public void userMenu(Account account, Service service) {
	userMenu.loop(service, account);
    }

    private void promptNewAccount(Service service) {
        boolean wasCreated = false;
        Account account = null;

	account = newAccountForm.collect(null);
	wasCreated = service.createAccount(account);

	if (!wasCreated) {
            console.printf("Account creation failed\n");
        }
        else {
            console.printf("Account created: %s\n", account);
        }
    }

    private void login(Service service) {

	final Optional<Account> maybeAccount =
	    loginForm.collect(service);

	if (!maybeAccount.isPresent()) {
	    return ;
        }
	final Account account = maybeAccount.get();
	this.userMenu(account, service);
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
    public void loop(Service service, Void args) {

	int choice = -1;
	while (choice != 0) {
	    choice = this.promptMenuChoice();
	    if (choice == 1) {
		this.login(service);
	    } else if (choice == 2) {
		this.promptNewAccount(service);
	    }
	}
    }
}

abstract class UserMenu<TypeAccount extends Account> extends Menu<TypeAccount> {

    protected Optional<TypeAccount> maybeUser = Optional.empty();
    
    UserMenu(IoAdapter console) {
	super(console);
    }

    protected void greeting(Account account) {
	console.printf("Welcome to ShellBank %s\n", account.getUserName());
    }
    
    protected void logOffSession() {
	maybeUser = Optional.empty();
    }

    protected void logInSession(TypeAccount account) {
	maybeUser = Optional.ofNullable(account);
    }

    protected void updateSession(TypeAccount updated) {
	if (!maybeUser.isPresent())
	    return ;
	final Account current = maybeUser.get();
	if (current.number != updated.number) {
	    throw new IllegalStateException(
					    "updateSession shold be called " +
					    "only whith updated copy of " +
					    "current Account");
	} else
	    this.maybeUser = Optional.of(updated);
    }
}

class AccountMenu extends UserMenu<Account> {

    private final ArrayList<Object> menus;
    
    AccountMenu(IoAdapter console, CheckingAccountMenu checkingAccountMenu, SavingAccountMenu savingAccountMenu) {
	super(console);
	menus = new ArrayList<Object>();
	menus.add(checkingAccountMenu);
	menus.add(savingAccountMenu);
    }

    private void checkingAccountMenu(Service service, CheckingAccount account) {

	CheckingAccountMenu menu = (CheckingAccountMenu) menus.get(0);
	menu.loop(service, account);
    }

    private void savingAccountMenu(Service service, SavingAccount account) {

	SavingAccountMenu menu = (SavingAccountMenu) menus.get(1);
	menu.loop(service, account);
    }
    
    @Override
    public Integer getMenuSize() {
	return -1; // loop delegated
    }
    
    @Override
    public String getMenuString() {
	return ""; // loop delegated
    }

    @Override
    public void loop(Service service, Account account) {

	if (account instanceof CheckingAccount) {
	    checkingAccountMenu(service, (CheckingAccount) account);
	} else if (account instanceof SavingAccount) {
	    savingAccountMenu(service, (SavingAccount) account);
	}
    }
}

class CheckingAccountMenu extends UserMenu<CheckingAccount> {

    private static final String startMenu =
	"Balance (1), Loan (2), Deposit (3), Withdraw (4), Back (0)\n";
    private final LoanIoForm loanForm;
    private final DepositIoForm depositForm;
    private final WithdrawIoForm withdrawForm;
    
    CheckingAccountMenu(
			IoAdapter console,
			LoanIoForm loanForm,
			DepositIoForm depositForm,
			WithdrawIoForm withdrawForm) {

	super(console);
	this.loanForm = loanForm;
	this.depositForm = depositForm;
	this.withdrawForm = withdrawForm;
    }
    
    private void balance(Account account) {
	console.printf("balance: %.2f\n", account.getBalance());
    }

    private void loan(Service service, CheckingAccount account) {
	double validatedLoanAmount = loanForm.collect(account);
	final Optional<CheckingAccount> maybeUpdated =
	    service.loan(validatedLoanAmount, account);

	if (!maybeUpdated.isPresent()) {
	    console.printf("Server Error: Loan was not created\n");
	} else {
	    console.printf("Loan amount is now available\n");
	    this.updateSession(maybeUpdated.get());
	}
    }

    private void deposit(Service service, CheckingAccount account) {
	
	double validatedLoanAmount = depositForm.collect(account);
	final Optional<Account> maybeUpdated =
	    service.deposit(validatedLoanAmount, account);

	if (!maybeUpdated.isPresent()
	    || !(maybeUpdated.get() instanceof CheckingAccount)) {
	    console.printf("Server Error: Deposit was not registered\n");
	} else {
	    console.printf("Deposit amount is now available\n");
	    this.updateSession((CheckingAccount) maybeUpdated.get());
	}
    }

    
    private void withdraw(Service service, CheckingAccount account) {
	
	double validatedLoanAmount = withdrawForm.collect(account);
	final Optional<Account> maybeUpdated =
	    service.withdraw(validatedLoanAmount, account);

	if (!maybeUpdated.isPresent()
	    || !(maybeUpdated.get() instanceof CheckingAccount)) {
	    console.printf("Server Error: Withdraw was not registered\n");
	} else {
	    console.printf("Withdraw amount is now available\n");
	    this.updateSession((CheckingAccount) maybeUpdated.get());
	}
    }

    @Override
    public Integer getMenuSize() {
	return 4;
    }
    
    @Override
    public String getMenuString() {
	return this.startMenu;
    }

    @Override
    public void loop(Service service, CheckingAccount account) {

	int choice = -1;

	this.logInSession(account);
	this.greeting(account);
	account = null;
	while (choice != 0 && maybeUser.isPresent()) {
	    final CheckingAccount current = maybeUser.get(); 
	    choice = this.promptMenuChoice();
	    if (choice == 1) {
		balance(current); 
	    } else if (choice == 2) {
		loan(service, current);
	    } else if (choice == 3) {
		deposit(service, current);
	    } else if (choice == 4) {
		withdraw(service, current);
	    }
	}
	this.logOffSession();
    }
}

class SavingAccountMenu extends UserMenu<SavingAccount> {

    private static final String startMenu =
	"Balance (1), Deposit (2), Withdraw (3), Back (0)\n";
    private final DepositIoForm depositForm;
    private final WithdrawIoForm withdrawForm;
    
    SavingAccountMenu(
		      IoAdapter console,
		      DepositIoForm depositForm,
		      WithdrawIoForm withdrawForm) {

	super(console);
	this.depositForm = depositForm;
	this.withdrawForm = withdrawForm;
    }

    private void balance(Account account) {
	console.printf("balance: %.2f\n", account.getBalance());
    }

    private void deposit(Service service, SavingAccount account) {
	
	double validatedLoanAmount = depositForm.collect(account);
	final Optional<Account> maybeUpdated =
	    service.deposit(validatedLoanAmount, account);

	if (!maybeUpdated.isPresent()
	    || !(maybeUpdated.get() instanceof SavingAccount)) {
	    console.printf("Server Error: Deposit was not registered\n");
	} else {
	    console.printf("Deposit amount is now available\n");
	    this.updateSession((SavingAccount) maybeUpdated.get());
	}
    }
    
    private void withdraw(Service service, SavingAccount account) {
	
	double validatedLoanAmount = withdrawForm.collect(account);
	final Optional<Account> maybeUpdated =
	    service.withdraw(validatedLoanAmount, account);

	if (!maybeUpdated.isPresent()
	    || !(maybeUpdated.get() instanceof SavingAccount)) {
	    console.printf("Server Error: Withdraw was not registered\n");
	} else {
	    console.printf("Withdraw amount is now available\n");
	    this.updateSession((SavingAccount) maybeUpdated.get());
	}
    }

    @Override
    public Integer getMenuSize() {
	return 3;
    }
    
    @Override
    public String getMenuString() {
	return this.startMenu;
    }

    @Override
    public void loop(Service service, SavingAccount account) {

	int choice = -1;

	this.logInSession(account);
	this.greeting(account);
	account = null;
	while (choice != 0 && maybeUser.isPresent()) {
	    final SavingAccount current = maybeUser.get(); 
	    choice = this.promptMenuChoice();
	    if (choice == 1) {
		balance(current); 
	    } else if (choice == 2) {
		deposit(service, current);
	    } else if (choice == 3) {
		withdraw(service, current);
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


class DepositIoForm extends IoForm<Double, Account> {

    public DepositIoForm(IoAdapter console) {
	super(console);
    }

    @Override
    public Double collect(Account account) {

	console.printf("Deposit:\n");
	while (true){
	    console.printf("How much have you deposited:\n");
	    final Optional<Double> maybeDepositAmount =
		console.readDoublerUnsigned();

	    if (!maybeDepositAmount.isPresent()) {
		console.printf("Invalid deposit\n");
		if (tryAgain()) {
		    continue ;
		}
		else
		    break ;
	    }
	    return maybeDepositAmount.get();
	}
	return 0.0;
    }
}

class WithdrawIoForm extends IoForm<Double, Account> {

    public WithdrawIoForm(IoAdapter console) {
	super(console);
    }

    @Override
    public Double collect(Account account) {

	console.printf("Withdraw:\n");
	while (true){
	    
	    console.printf("How much would you like to withdraw:\n");
	    console.printf("Balance: %.2f\n", account.getBalance());
	    final Optional<Double> maybeWithdrawAmount =
		console.readDoublerUnsigned();

	    if (!maybeWithdrawAmount.isPresent()
		|| !account.isValidWithdraw(maybeWithdrawAmount.get())) {
		console.printf("Invalid withdraw\n");
		if (tryAgain()) {
		    continue ;
		}
		else
		    break ;
	    }
	    return maybeWithdrawAmount.get();
	}
	return 0.0;
    }
}


class LoanIoForm extends IoForm<Double, Account> {

    public LoanIoForm(IoAdapter console) {
	super(console);
    }

    @Override
    public Double collect(Account account) {

	if (!(account instanceof CheckingAccount))
	    return 0.0;
	final CheckingAccount checkingAccount = (CheckingAccount) account;
	account = null;
	
	console.printf("Loan:\n");
	while (true){
	    console.printf(
			   "Max loan available: %.2f\n",
			   checkingAccount.getLoanLimit());
	    console.printf("Min loan: %.2f\n", 1.0); 

	    final Optional<Double> maybeLoanRequired =
		console.readDoublerUnsigned();

	    if (!checkingAccount.isValidLoanRequest(maybeLoanRequired)
		|| !maybeLoanRequired.isPresent()) {

		console.printf("Invalid loan requested\n");
		if (tryAgain()) {    
		    continue ;
		}
		else
		    break ;
	    }
	    return maybeLoanRequired.get();
	}
	return 0.0;
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
	    console.printf("Choose account type: Checking (1), Saving (2)\n");
	    final int type = console.readNumberUnsigned();
	    if (type < 1 || type > 2) {
		console.printf("Invalid account type\n");
		if (tryAgain())
		    continue;
		else
		    break;
	    }

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
	    if (type == 1)
		account = new CheckingAccount(name, pass);
	    else if (type == 2)
		account = new SavingAccount(name, pass);
	    else
		throw new IllegalStateException("Invalid account type " +
						"while creating new account");
	    break ;
	}
	return account;
    }
}

class LoginIoForm extends IoForm<Optional<Account>, Service> {

    public LoginIoForm(IoAdapter console) {
	super(console);
    }

    @Override
    public Optional<Account> collect(Service service) {
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

	maybeAccount = service.getAccountByNumber(numberAccount);
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

    public void mainMenu(Service service) {
	mainMenu.displayWelcome();
	mainMenu.loop(service, null);
	mainMenu.displayBye();
    }
}

class Main {

    static public Presenter defaultPresenter(IoAdapter ioAdapter) {

	final NewAccountIoForm newAccountForm = new NewAccountIoForm(ioAdapter);
	final LoginIoForm loginForm = new LoginIoForm(ioAdapter);

	final LoanIoForm loanForm = new LoanIoForm(ioAdapter);
	final DepositIoForm depositForm = new DepositIoForm(ioAdapter);
	final WithdrawIoForm withdrawForm = new WithdrawIoForm(ioAdapter);
	
	final CheckingAccountMenu chekingAccountMenu =
	    new CheckingAccountMenu(
				    ioAdapter,
				    loanForm,
				    depositForm,
				    withdrawForm);

	final SavingAccountMenu savingAccountMenu =
	    new SavingAccountMenu(
				  ioAdapter,
				  depositForm,
				  withdrawForm);

	final AccountMenu accountMenu =
	    new AccountMenu(ioAdapter, chekingAccountMenu, savingAccountMenu);
	final MainMenu mainMenu = new MainMenu(
					       ioAdapter,
					       newAccountForm,
					       loginForm,
					       accountMenu
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
	final Service service = new Service(repository);
	final Presenter presenter = defaultPresenter(ioAdapter);

        presenter.mainMenu(service);
    }
}
