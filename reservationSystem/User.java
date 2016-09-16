package reservationSystem;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;

import reservationSystem.ReservationControlCenter.resVSingleUser;
import sun.java2d.StateTrackable.State;

import com.sun.jmx.remote.internal.ArrayQueue;

public class User {
	public final static double freewaySpeed=26.8224; //meters per second
	public final static double artLSpeed=8.941; //meters per second
	public final static double connectorSpeed=8.941; //meters per second
	public final static double freewayLinkLength=3218.69; //meters
	public final static double artLLinkLength=3218.69; //meters
	public final static double connectorLength=500; //meters
	
	private int userID;
	private int origin;
	private int destination;
	private double earlyArrivalVOT;
	private double travelTimeVOT;
	private double lateArrivalVOT;
	private int dat;
	private int mostDesiredInterval;
	private double delta;//bidding reduction amount
	private double alpha;// percentage of coming to reservation system
	private double altTravelCost;
	
	private memory mem;
	private profile prof;
	
	public enum StateSet
	{
		Initial, Decreasing, Increasing, Stable, ALT
	}
	
	private LinkedList<memory> altMemory=new LinkedList<memory>();
	private LinkedList<memory> resMemory=new LinkedList<memory>();
	public class profile
	{
		int stableInterval=-1;
		double stableBid=-1;
		public int getStableInterval() {
			return stableInterval;
		}
		public void setStableInterval(int stableInterval) {
			this.stableInterval = stableInterval;
		}
		public double getStableBid() {
			return stableBid;
		}
		public void setStableBid(double stableBid) {
			this.stableBid = stableBid;
		}	
	}
	public void createProfile()
	{
		prof=new profile();
	}
	public void deleteProfile()
	{
		prof=null;
	}
	public class memory
	{
		int iter0;
		StateSet state0;
		int biddingInterval0;
		double biddingAmount0;
		boolean result0=false;
		double depTime; // in seconds
		double arrTime; // in seconds
		double travelCost;
		public int getIter0() {
			return iter0;
		}
		public void setIter0(int iter0) {
			this.iter0 = iter0;
		}
		public StateSet getState0() {
			return state0;
		}
		public void setState0(StateSet state0) {
			this.state0 = state0;
		}
		public int getBiddingInterval0() {
			return biddingInterval0;
		}
		public void setBiddingInterval0(int biddingInterval0) {
			this.biddingInterval0 = biddingInterval0;
		}
		public double getBiddingAmount0() {
			return biddingAmount0;
		}
		public void setBiddingAmount0(double biddingAmount0) {
			this.biddingAmount0 = biddingAmount0;
		}
		public boolean isResult0() {
			return result0;
		}
		public void setResult0(boolean result0) {
			this.result0 = result0;
		}
		public double getDepTime() {
			return depTime;
		}
		public void setDepTime(double depTime) {
			this.depTime = depTime;
		}		
		public double getArrTime() {
			return arrTime;
		}
		public void setArrTime(double arrTime) {
			this.arrTime = arrTime;
		}
		public double getTravelCost() {
			return travelCost;
		}
		public void setTravelCost(double travelCost) {
			this.travelCost = travelCost;
		}		
	}
	public void createMem()
	{
		mem=new memory();
	}
	public void deleteMem()
	{
		mem=null;
	}

	public void logic1(int ite,ReservationControlCenter controlCenter) //blind search
	{
		boolean previousResult;
		int previousInterval=-1;
		double previousBid=-1;
		StateSet previousState;
		double maxBidAmount=0;
		double previousBidTimeCost=-1;
		int key=this.origin*10000+this.destination*1000+this.dat;
		double altCostTemp=0;
		//****************************************************  Very Important!!!  ****************************************************////
	    //********************  Arterial cost is based on previous iteration's average dep time and arr time  *************************////
		//if(this.getAltMemory().size()>0 && this.getAltMemory().getLast().getIter0()==ite-1)
		//	this.altTravelCost=this.getAltMemory().getLast().getTravelCost();
		//else
		//{
			if(controlCenter.getAvgAltTravelCost().get(key).getCount()>0)
			{
				double avgDepInt=controlCenter.getAvgAltTravelCost().get(key).getAvgDepartInterval();
				double avgArrInt=controlCenter.getAvgAltTravelCost().get(key).getAvgArrivInterval()+
						controlCenter.getAvgAltTravelCost().get(key).getStdArrivInterval()*2;
				if(avgArrInt>this.dat)
					altCostTemp=(avgArrInt-avgDepInt)/30*this.travelTimeVOT+(avgArrInt-this.dat)/30*this.lateArrivalVOT;
				else
					altCostTemp=(avgArrInt-avgDepInt)/30*this.travelTimeVOT+(this.dat-avgArrInt)/30*this.earlyArrivalVOT;
			}
			else
			{
			    //********************  Very Important!!!  *************************////
				altCostTemp=((this.getDestination()-this.getOrigin())*User.artLLinkLength+2*User.connectorLength)/controlCenter.getArtAvgSpd()/3600*this.getTravelTimeVOT();
				//********************  Use overall average speed  *************************////
			}
		//}
			//**************Smooth the altTravelCost*********************************//
			this.altTravelCost=this.altTravelCost*0.5+altCostTemp*0.5;
		//***************************Very Important *************************************
		this.delta=this.altTravelCost*0.07;
		//****************************************************************
		if(this.getResMemory().size()>0 && this.getResMemory().getLast().getIter0()==ite-1)
		{
			previousResult=true;
			previousInterval=this.getResMemory().getLast().getBiddingInterval0();
			double travelTime=(this.getResMemory().getLast().getArrTime()-this.getResMemory().getLast().getDepTime())/3600;
			//double travelTime=((this.getDestination()-this.getOrigin())*3218.7/User.freewaySpeed+2*500/connectorSpeed)/3600; //in hours
			//previousBidTimeCost=travelTime*this.getTravelTimeVOT()+(this.getDat()-previousInterval-travelTime*30)/30*this.getEarlyArrivalVOT();
			previousBidTimeCost=travelTime*this.getTravelTimeVOT()+(this.getDat()-(this.getResMemory().getLast().getArrTime()-6*3600)/120)/30*this.getEarlyArrivalVOT();
			//********* Use the estimated speed**********//			
			previousBid=this.getResMemory().getLast().getBiddingAmount0();
			previousState=this.getResMemory().getLast().getState0();
		}
		else
		{
			previousResult=false;
			previousState=this.getAltMemory().getLast().getState0();
			
			if(this.getAltMemory().getLast().getState0()!=StateSet.ALT)
			{
				previousInterval=this.getAltMemory().getLast().getBiddingInterval0();
				double travelTime=((this.getDestination()-this.getOrigin())*3218.7/User.freewaySpeed+2*500/connectorSpeed)/3600; //in hours
				previousBidTimeCost=travelTime*this.getTravelTimeVOT()+(this.getDat()-previousInterval-travelTime*30)/30*this.getEarlyArrivalVOT();
				previousBid=this.getAltMemory().getLast().getBiddingAmount0();					
			}
		}
		maxBidAmount=altTravelCost-previousBidTimeCost;
		this.deleteMem();
		this.createMem();
		this.getMem().setIter0(ite);

		if(previousState==StateSet.Initial && previousResult) //from initial state to decreasing state
		{
			if(previousBid<this.getDelta())
			{
				this.getProf().setStableInterval(previousInterval);
				this.getProf().setStableBid(previousBid);
				this.getMem().setBiddingInterval0(previousInterval);
				this.getMem().setBiddingAmount0(previousBid);
				this.getMem().setState0(StateSet.Stable);
			}
			else 
			{				
				this.getMem().setBiddingInterval0(previousInterval);
				this.getMem().setBiddingAmount0(previousBid-this.delta);
				this.getMem().setState0(StateSet.Decreasing);
			}
		}
		if((previousState==StateSet.Initial || previousState==StateSet.Increasing || previousState==StateSet.Stable) && !previousResult)
		{
			if(previousBid+this.delta>maxBidAmount)
			{
				if(maxBidAmount-this.getEarlyArrivalVOT()*1/30<0)
				{
					this.getMem().setState0(StateSet.ALT);
					this.getProf().setStableInterval(-1);
					this.getProf().setStableBid(-1);
					this.getMem().setBiddingInterval0(-1);
					this.getMem().setBiddingAmount0(-1);
				}
				else
				{
					this.getMem().setBiddingInterval0(previousInterval-1);
					this.getMem().setBiddingAmount0(maxBidAmount-this.getEarlyArrivalVOT()*1/30);
					this.getMem().setState0(StateSet.Initial);
				}
			}
			else
			{				
				this.getMem().setBiddingInterval0(previousInterval);
				this.getMem().setBiddingAmount0(previousBid+this.delta);
				this.getMem().setState0(StateSet.Increasing);
			}
		}
		if(previousState==StateSet.Decreasing && previousResult)
		{
			if(previousBid<this.delta)
			{
				this.getMem().setState0(StateSet.Stable);
				this.prof.setStableInterval(previousInterval);
				this.prof.setStableBid(previousBid);
				this.getMem().setBiddingInterval0(previousInterval);
				this.getMem().setBiddingAmount0(previousBid);
			}
			else
			{
				this.getMem().setState0(StateSet.Decreasing);
				this.getMem().setBiddingInterval0(previousInterval);
				this.getMem().setBiddingAmount0(Math.min(previousBid-this.delta,maxBidAmount));
			}
		}
		if(previousState==StateSet.Decreasing && !previousResult)
		{
			if(ite>2 && this.getResMemory().getLast().getIter0()==ite-2)
			{
				this.getMem().setState0(StateSet.Stable);
				this.getProf().setStableInterval(this.getResMemory().getLast().getBiddingInterval0());
				this.getProf().setStableBid(this.getResMemory().getLast().getBiddingAmount0());
				this.getMem().setBiddingInterval0(this.getProf().getStableInterval());
				this.getMem().setBiddingAmount0(this.getProf().getStableBid());
			}
			else
			{
				System.out.println("Something is wrong.Plz double check");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} //If something is wrong pause for 200 seconds
			}
		}
		if(previousState==StateSet.Increasing && previousResult)
		{
			this.getMem().setState0(StateSet.Stable);
			this.prof.setStableInterval(previousInterval);
			this.prof.setStableBid(previousBid);
			this.getMem().setBiddingInterval0(previousInterval);
			this.getMem().setBiddingAmount0(previousBid);
		}
		if(previousState==StateSet.Stable && previousResult)
		{
			//**********************altTravelCost uses previous average, current. Temporary solve********//
			//*********************the factor is very important!! changed from 1.4 to 0.9*********//
			if(previousBid+previousBidTimeCost>altTravelCost) 
			{
				this.getMem().setState0(StateSet.Initial);
				this.getMem().setBiddingInterval0(this.prof.getStableInterval());
				this.getMem().setBiddingAmount0(Math.max(0,altTravelCost-previousBidTimeCost));
			}
			else
			{
				this.getMem().setState0(StateSet.Stable);
				this.getMem().setBiddingInterval0(this.prof.getStableInterval());
				this.getMem().setBiddingAmount0(this.prof.getStableBid());
			}
		}
		if(previousState==StateSet.ALT)
		{
			if(Math.random()>=alpha)
			{
				this.getMem().setState0(StateSet.ALT);
				this.getMem().setBiddingInterval0(-1);
				this.getMem().setBiddingAmount0(-1);
			}
			else
			{
				this.getMem().setState0(StateSet.Initial);
				this.getMem().setBiddingInterval0(this.getMostDesiredInterval());
				this.getMem().setBiddingAmount0(altTravelCost-((this.getDestination()-this.getOrigin())*User.freewayLinkLength/User.freewaySpeed+2*500/User.connectorSpeed)/3600*this.getTravelTimeVOT());
			}
		}
		if(this.getMem().getState0()!=StateSet.ALT)
			controlCenter.addResVUser(this.userID, this.getMem().getBiddingAmount0());
	}
	public void deptTimeLogic3(ReservationControlCenter controlCenter)
	{
		int DAT=this.getDat();
		int O=this.getOrigin();
		int D=this.getDestination();
		double seconds=0;
		if(this.getMem().getState0()!=StateSet.ALT && this.getMem().isResult0())
		{
			int departTimeInterval=this.getMem().getBiddingInterval0();
			seconds=(int)Math.round((departTimeInterval+6*30)*120-Math.random()*120);			
		}
		else
		{
			int key=this.origin*10000+this.destination*1000+this.dat;
			if(controlCenter.getAvgAltTravelCost().get(key).getCount()>0)
			{
				//****************very important. How arterial departure time is determined****************************//
				double avgDepInt=controlCenter.getAvgAltTravelCost().get(key).getAvgDepartInterval();
				double avgArrInt=controlCenter.getAvgAltTravelCost().get(key).getAvgArrivInterval();
				if(avgArrInt>this.dat+2)
					seconds=(avgDepInt-(avgArrInt-this.dat)*0.3+(Math.random()-0.5)*5)*120+6*3600;
				else
					if(avgArrInt<this.dat-5)
						seconds=(avgDepInt+(this.dat-5-avgArrInt)*0.3+(Math.random()-0.5)*5)*120+6*3600;
					else
						seconds=(avgDepInt+(Math.random()-0.5)*5)*120+6*3600;
				//****************very important. How arterial departure time is determined****************************//
			}
			else
			{
				seconds=(DAT-((D-O)*this.artLLinkLength+2*this.connectorLength)/controlCenter.getArtAvgSpd()/120-Math.random()*3)*120+6*3600;				
			}
		}
		this.getMem().setDepTime(seconds);				
	}
	public void deptTimeLogic2(ReservationControlCenter controlCenter)
	{
		int DAT=this.getDat();
		int O=this.getOrigin();
		int D=this.getDestination();
		double seconds=0;
		if(this.getMem().getState0()!=StateSet.ALT && this.getMem().isResult0())
		{
			int departTimeInterval=this.getMem().getBiddingInterval0();
			seconds=(int)Math.round((departTimeInterval+6*30)*120-Math.random()*120);			
		}
		else
		{
			int key=this.origin*10000+this.destination*1000+this.dat;
			if(controlCenter.getAvgAltTravelCost().get(key).getCount()>0)
			{
				double avgDepInt=controlCenter.getAvgAltTravelCost().get(key).getAvgDepartInterval();
				double avgArrInt=controlCenter.getAvgAltTravelCost().get(key).getAvgArrivInterval();
				seconds=(this.getDat()-(avgArrInt-avgDepInt)-Math.random()*5)*120+6*3600;
			}
			else
			{
				seconds=(DAT-((D-O)*this.artLLinkLength+2*this.connectorLength)/controlCenter.getArtAvgSpd()/120-Math.random()*5)*120+6*3600;				
			}
		}
		this.getMem().setDepTime(seconds);				
	}
	public void deptTimeLogic1()
	{
		if(this.getMem().getState0()!=StateSet.ALT && this.getMem().isResult0())
		{
			int departTimeInterval=this.getMem().getBiddingInterval0();
			double seconds=(int)Math.round((departTimeInterval+6*30)*120-Math.random()*120);
			this.getMem().setDepTime(seconds);
		}
		else
		{
			if(this.getAltMemory().size()<1) //no arterial experience yet. Use overall average speed
			{
				int DAT=this.getDat();
				int O=this.getOrigin();
				int D=this.getDestination();
				double departTimeInterval=DAT-((D-O)*this.artLLinkLength/this.artLSpeed+2*this.connectorLength/this.connectorSpeed)/120-Math.random()*20;
				double seconds=Math.round((departTimeInterval+6*30)*120);		
				this.getMem().setDepTime(seconds);
			}
			else
			{
				double previousDepartInterval=(this.getAltMemory().getLast().getDepTime()-6*3600)/120;
				double previousArrivInterval=(this.getAltMemory().getLast().getArrTime()-6*3600)/120;
				double departTimeInterval;
				if(previousArrivInterval-this.getDat()<6 || this.getDat()-previousArrivInterval<10) //meaning being late last time
				{
					departTimeInterval=previousDepartInterval;
				}
				else
				{
					if(previousArrivInterval-this.getDat()>=6) //The 20 here could vary by traveler
						if(previousDepartInterval-this.getMostDesiredInterval()>-30)
							departTimeInterval=previousDepartInterval-1;
						else
							departTimeInterval=previousDepartInterval;
					else
						departTimeInterval=previousDepartInterval+1;
				}
				this.getMem().setDepTime(departTimeInterval*120+6*3600+(Math.random()-0.5)*120);
			}
		}
	}
	//	getters and setters	
	public memory copyMemory()
	{
		memory newM=new memory();
		newM.setArrTime(this.getMem().getArrTime());
		newM.setBiddingAmount0(this.getMem().getBiddingAmount0());
		newM.setBiddingInterval0(this.getMem().getBiddingInterval0());
		newM.setDepTime(this.getMem().getDepTime());
		newM.setIter0(this.getMem().getIter0());
		newM.setResult0(this.getMem().isResult0());
		newM.setState0(this.getMem().getState0());
		newM.setTravelCost(this.getMem().getTravelCost());
		return newM;
	}
	public int getUserID() {
		return userID;
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	public int getOrigin() {
		return origin;
	}
	public void setOrigin(int origin) {
		this.origin = origin;
	}
	public int getDestination() {
		return destination;
	}
	public void setDestination(int destination) {
		this.destination = destination;
	}
	public double getEarlyArrivalVOT() {
		return earlyArrivalVOT;
	}
	public void setEarlyArrivalVOT(double earlyArrivalVOT) {
		this.earlyArrivalVOT = earlyArrivalVOT;
	}
	public double getTravelTimeVOT() {
		return travelTimeVOT;
	}
	public void setTravelTimeVOT(double travelTimeVOT) {
		this.travelTimeVOT = travelTimeVOT;
	}
	public double getLateArrivalVOT() {
		return lateArrivalVOT;
	}
	public void setLateArrivalVOT(double lateArrivalVOT) {
		this.lateArrivalVOT = lateArrivalVOT;
	}
	public int getDat() {
		return dat;
	}
	public void setDat(int dat) {
		this.dat = dat;
	}
	public LinkedList<memory> getAltMemory() {
		return altMemory;
	}
	public void setAltMemory(LinkedList<memory> altMemory) {
		this.altMemory = altMemory;
	}
	public int getMostDesiredInterval() {
		return mostDesiredInterval;
	}
	public void setMostDesiredInterval(int mostDesiredInterval) {
		this.mostDesiredInterval = mostDesiredInterval;
	}
	public LinkedList<memory> getResMemory() {
		return resMemory;
	}
	public void setResMemory(LinkedList<memory> resMemory) {
		this.resMemory = resMemory;
	}
	public memory getMem() {
		return mem;
	}
	public void setMem(memory mem) {
		this.mem = mem;
	}
	public double getDelta() {
		return delta;
	}
	public void setDelta(double delta) {
		this.delta = delta;
	}	
	public double getAlpha() {
		return alpha;
	}
	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}
	public profile getProf() {
		return prof;
	}
	public void setProf(profile prof) {
		this.prof = prof;
	}
	public double getAltTravelCost() {
		return altTravelCost;
	}
	public void setAltTravelCost(double altTravelCost) {
		this.altTravelCost = altTravelCost;
	}
}


