
public class BadRequestException extends Exception {
	
	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super("Illegal client input");
	}
	
	public BadRequestException(String s) {
		super("Illegal client input");
	}
}
