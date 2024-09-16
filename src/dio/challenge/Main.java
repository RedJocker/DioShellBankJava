package dio.challenge;

import java.io.Console;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import org.mindrot.jbcrypt.BCrypt;


interface Repository {
    public Optional<Account> getAccountByNumber(int accountNumber);
    public boolean saveAccount(Account account);
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

class Presenter {
    private final String wellcome = "Wellcome to ShellBank\n";
    private final String startMenu = "Login (1), NewAccount (2), Exit (0)\n";
    private final String invalidChoice = "Invalid Choice\n";
    private final String promptName = "Please type your name\n";

    private final Console console;

    public Presenter(Console console) {
        this.console = console;
    }

    public void displayWellcome() {
        console.printf(this.wellcome);
    }

    public int promptStartMenu() {
        int option;

        console.printf(this.startMenu);
        option = this.readNumberUnsigned();
        if (option < 0 || option > 2) {
            console.printf(this.invalidChoice);
            return promptStartMenu();
        } else {
            return option;
        }
    }
    
    private int readNumberUnsigned() {
	String inputLine;
        int option;

        inputLine = console.readLine();
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
    
    private String readPass(String prompt) {
        char pass[] = console.readPassword(prompt);
        return pass == null ? null : String.valueOf(pass);
    }

    public void promptNewAccount(Repository repository) {
        boolean wasCreated;
        String name;
        Account account = null;
        String pass;
        String confirm;
        String tryAgain;

        wasCreated = false;
        console.printf("New Account:\n");
        name = console.readLine(this.promptName);
        if (name == null || name.isBlank()) {
            console.printf("Invalid name\n");
        } else {
            tryAgain = null;
            while (true){
                pass = this.readPass("Choose a 4 digit number password");
                if (pass == null || !pass.matches("\\d{4}")) {
                    console.printf("Invalid password\n");
                    tryAgain = console.readLine("Quit (0) TryAgain (Any)\n");
                    if (tryAgain == null || tryAgain.equals("0"))
                        break;
                    else
                        continue;
                }
                confirm = this.readPass("Confirm password");
                if (confirm == null || !confirm.equals(pass)) {
                    console.printf("Invalid confirmation\n");
                    tryAgain = console.readLine("Quit (0) TryAgain (Any)\n");
                    if (tryAgain == null || tryAgain.equals("0"))
                        break;
                    else
                        continue;
                }
                account = new CheckingAccount(name, pass);
                wasCreated = repository.saveAccount(account);
                break ;
            }
        }

        if (!wasCreated && account == null) {
            console.printf("Account creation failed\n");
        }
        else {
            console.printf("Account created: %s\n", account);
        }
    }

    public void login(Repository repository) {
        int numberAccount;
        Optional<Account> maybeAccount;
	Account account;
        String pass;

        console.printf("Login:\n");
	console.printf("Number Account: ");
	numberAccount = this.readNumberUnsigned();
	if (numberAccount < 0) {
            console.printf("Account not found\n");
	    return ;
        }
	maybeAccount = repository.getAccountByNumber(numberAccount);

	if (!maybeAccount.isPresent()) {
            console.printf("Account not found\n");
	    return ;
        }
	account = maybeAccount.get();
        pass = this.readPass("Enter 4 digit number password");
        if (!account.verifyPass(pass)) {
            console.printf("Invalid password\n");
	    return ;
	}
	this.userMenu(account, repository);
    }

    private int promptUserMenu() {
	return 0;
    }

    public void userMenu(Account account, Repository repository) {
	console.printf("Bem Vindo %s\n", account.getUserName());

	int choice = -1;
        while (choice != 0) {
            choice = this.promptUserMenu();
        }
    }
}

class Main {
    public static void main(String[] args) {
        final Presenter presenter = new Presenter(System.console());
        final Repository repository = new RepositoryInMemory(new HashMap<>());

        presenter.displayWellcome();
        int choice = -1;
        while (choice != 0) {
            choice = presenter.promptStartMenu();
	    if (choice == 1) {
		presenter.login(repository);
	    } else if (choice == 2) {
                presenter.promptNewAccount(repository);
            }
        }
        System.out.println("Bye");
    }
}
