package crawler;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class ArgsTest {

	public static void main(String[] args) {

		// create Options object
		Options options = new Options();

		// add t option, True means a value is required, false means just a flag
		options.addOption("t", true, "display current time");

		CommandLine cmd = null;
		CommandLineParser parser = new DefaultParser();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		if (cmd.hasOption("t")) {
			System.out.println("T flag has been used with value " + cmd.getOptionValue("t"));

		} else {
			System.out.println("No flag has been used");
		}

	}
}
