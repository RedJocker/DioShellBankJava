package dio.challenge;

import java.util.Scanner;

interface Account {
    public int getNumber();
    public String getBranch();
    public String getUserName();
    public double getBalance();
}

class CheckingAccount implements Account {
    public int getNumber() {
	return 0;
    }
    public String getBranch() {
	return "";
    }
    public String getUserName() {
	return "";
    }
    public double getBalance() {
	return 0.0;
    }
}

class Presenter {
    private final String wellcome = "Wellcome to ShellBank";
    private final String startMenu = "Login (1), NewAccount (2), Exit (0)";
    private final String invalidChoice = "Invalid Choice";
    private final Scanner scanner;

    public Presenter(Scanner scanner) {
	this.scanner = scanner;
    }
    
    public void displayWellcome() {
	System.out.println(this.wellcome);
    }

    public int promptStartMenu() {
	String inputLine;
	int option;

	System.out.println(this.startMenu);
	if (!scanner.hasNext()) {
	    option = 0;
	} else {
	    inputLine = scanner.next();
	    try {
		option = Integer.parseInt(inputLine);
		if (option < 0 || option > 2) {
		    option = -1;
		}
	    } catch (NumberFormatException ex) {
		option = -1;
	    }
	}
	
	if (option < 0) {
	    System.out.println(this.invalidChoice);
	    return promptStartMenu();
	} else {
	    return option;
	}
    }
}

class Main {
    public static void main(String[] args) {
	final Presenter presenter = new Presenter(new Scanner(System.in));
	presenter.displayWellcome();
	int choice = presenter.promptStartMenu();
	System.out.println(String.format("The choice was %d", choice));
    }
}
