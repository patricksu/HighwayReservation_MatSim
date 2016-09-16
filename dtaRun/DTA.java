package dtaRun;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
public class DTA {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		System.out.println("OK Let's Go"+"\n");
		Config config = ConfigUtils.loadConfig("input/MyConfig_WorkSheet.xml");
		Controler controler = new Controler(config);
		// controler.setOverwriteFiles(true);
		controler.run();
	}
}
