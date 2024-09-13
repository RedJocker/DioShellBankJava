package dio.challenge;

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

class Main {
    public static void main(String[] args) {
	System.out.println("Hello");
    }
}
