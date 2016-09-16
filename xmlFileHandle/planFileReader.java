package xmlFileHandle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.matsim.api.core.v01.Id;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

public class planFileReader {
//There is no need to read the plan file to get route info. Event file will do it.
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		// TODO Auto-generated method stub
		 final String fileName = "run0.0.plans.xml";
	        Document document;
	        DocumentBuilder documentBuilder;
	        DocumentBuilderFactory documentBuilderFactory;
	        NodeList personNodeList;
	        File xmlInputFile;
	        Map<Integer, Set<Integer>> personRoutes=new HashMap<Integer, Set<Integer>>();
	        Map<Integer, Integer> personArrivalTime=new HashMap<Integer, Integer>();
	        Map<Integer, Integer> personDepartureTime=new HashMap<Integer, Integer>();
	        try
	        {
	            xmlInputFile = new File(fileName);
	            documentBuilderFactory = DocumentBuilderFactory.newInstance();
	            documentBuilder = documentBuilderFactory.newDocumentBuilder();
	            document = documentBuilder.parse(xmlInputFile);
	            personNodeList = document.getElementsByTagName("person");
	            System.out.println("Total no of persons : " + personNodeList.getLength());
	            
	            document.getDocumentElement().normalize();
	            int personID, time;
	            String linkID;
	            String actType;
	            for (int index = 0; index < personNodeList.getLength(); index++)
	            {
	                Node nodePerson = personNodeList.item(index);
	                boolean Freeway;
                    Element elementPerson = (Element) nodePerson;
                    NodeList planNodeList=elementPerson.getElementsByTagName("plan");
                    for(int planIndex=0;planIndex<planNodeList.getLength();planIndex++)
                    {
                    	Node nodePlan=planNodeList.item(planIndex);
                    	Element elementPlan=(Element) nodePlan;
                    	if(elementPlan.getAttribute("selected").equals("yes"))
                    	{
	                    	NodeList legNodeList=elementPlan.getElementsByTagName("leg");
	                    	Element elementLeg=(Element) legNodeList.item(0);
	                    	NodeList routeNodeList=elementLeg.getElementsByTagName("route");
	                    	Element elementRoute=(Element) routeNodeList.item(0);
	                    	String route=elementRoute.getTextContent();
	                    	String[] routeLinks=route.split(" ");
	                    	int thirdLink=Integer.parseInt(routeLinks[2]);
	                    	if(thirdLink<=6)
	                    		Freeway=true;
	                    	else
	                    		Freeway=false;
	                    	break;
                    	}
                    }                                       
                    personID=Integer.valueOf(elementPerson.getAttribute("person"));
                    linkID=elementPerson.getAttribute("link");
                    time=(int)Double.parseDouble((elementPerson.getAttribute("time")));
                    actType=elementPerson.getAttribute("type");
                    Set<Integer> route=personRoutes.get(personID);          
                    if(linkID.length()>0)
                    {
                    	if(route==null)
                    		route=new TreeSet<Integer>();
                    	route.add(Integer.valueOf(linkID));
                    	personRoutes.put(personID, route);                    	
                    }
                    if(actType.equals("actstart"))
                    {
                    	personArrivalTime.put(personID, time);
                    }
                    if(actType.equals("actend"))
                    {
                    	personDepartureTime.put(personID, time);
                    }                    
                    //System.out.println("\t time : " + element.getAttribute("time"));
                    //System.out.println("\t type : " + element.getAttribute("type"));
                    //System.out.println("\t person : " + element.getAttribute("person"));
                    //System.out.println("\t link : " + element.getAttribute("link"));
                    //System.out.println("\t actType : " + element.getAttribute("actType"));
                    //System.out.println("-----");
	            }
	            int i=3;
	            i=i+3;
	        }
	        catch (Exception exception)
	        {
	            exception.printStackTrace();
	        }
	        
	        PrintWriter outputStream=new PrintWriter(new FileOutputStream ("out_route.txt",true));
	        for(Map.Entry<Integer, Set<Integer>> entry : personRoutes.entrySet())
	        {
	        	Integer personID=entry.getKey();
	        	Set<Integer> route=entry.getValue();         	
	        	outputStream.print(personID+"\t");
	        	for(Integer link:route)
	        	{
	        		outputStream.print(link+"\t");
	        	}
	        	outputStream.print("\n");
	        }
	        outputStream.flush();
	        outputStream.close();
	        
	        PrintWriter outputStream2=new PrintWriter(new FileOutputStream ("out_arrivaltime.txt",true));
	        for(Map.Entry<Integer, Integer> entry : personArrivalTime.entrySet())
	        {      	
	        	outputStream2.print(entry.getKey()+"\t");
	        	outputStream2.print(entry.getValue()+"\t");
	        	outputStream2.print("\n");
	        }
	        outputStream2.flush();
	        outputStream2.close();
	        
	        PrintWriter outputStream3=new PrintWriter(new FileOutputStream ("out_departuretime.txt",true));
	        for(Map.Entry<Integer, Integer> entry : personDepartureTime.entrySet())
	        {      	
	        	outputStream3.print(entry.getKey()+"\t");
	        	outputStream3.print(entry.getValue()+"\t");
	        	outputStream3.print("\n");
	        }
	        outputStream3.flush();
	        outputStream3.close();

	}

}
