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

public class DOMParser{

    public static void main (String argv []) throws FileNotFoundException{
        final String fileName = "run0.0.events.xml";
        Document document;
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        NodeList nodeList;
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
            nodeList = document.getElementsByTagName("event");
            System.out.println("Total no of events : " + nodeList.getLength());

            document.getDocumentElement().normalize();
            int personID, time;
            String linkID;
            String actType;
            for (int index = 0; index < nodeList.getLength(); index++)
            {
                Node node = nodeList.item(index);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) node;
                    personID=Integer.valueOf(element.getAttribute("person"));
                    linkID=element.getAttribute("link");
                    time=(int)Double.parseDouble((element.getAttribute("time")));
                    actType=element.getAttribute("type");
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
                }
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