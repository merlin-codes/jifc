public class Main {
	public static void main(String[] args) {
		try {
			ParseClassFile.parseClassFile(args[0]);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.out.println("Error parsing class file named: main.class");
		}
	}
}
