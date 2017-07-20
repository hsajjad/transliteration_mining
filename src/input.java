import java.io.*;
import java.lang.*;
import java.util.*;

public class input {
	public void getUserInput(ArrayList input, String File) {

		try {
			BufferedReader is = new BufferedReader(new FileReader(File));

			String inputLine;
			while ((inputLine = is.readLine()) != null) {
				input.add(inputLine.toLowerCase());
			}
			is.close();

		} catch (IOException e) {
			System.out.println("IOException: " + e);
			System.exit(1);
		}
	}
}