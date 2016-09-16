package tutorial;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;

public class MyFirstControler {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int i=1;
		i=i+1;
		System.out.println("\""+"d");
		// TODO Auto-generated method stub
		Config config = ConfigUtils.loadConfig("input/MyConfig.xml");
		Controler controler = new Controler(config);
		// controler.setOverwriteFiles(true);
		controler.run();
	}
}
