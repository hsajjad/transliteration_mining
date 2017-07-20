import java.io.*;

public class Output {
	public void printOutput(String str, Boolean a, String file) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file, a));
            out.write(str);
            out.write('\n');
            out.flush();
            out.close();
        } catch (IOException e) {
		}
	}
}