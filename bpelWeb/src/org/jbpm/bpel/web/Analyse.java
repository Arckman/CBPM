package org.jbpm.bpel.web;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jbpm.JbpmConfiguration;
import org.jbpm.bpel.graph.def.BpelProcessDefinition;

/**
 * Servlet implementation class Analyse
 */
public class Analyse extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public Analyse() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		 String processName=(String) request.getParameter("processName");
		 String activityName=(String) request.getParameter("activityName");
		 String stopActivity=(String)request.getParameter("stopActivity");
//		 if(processName!=null&&activityName!=null){
//			 JbpmConfiguration.addConcernedActivity(processName, activityName,stopActivity);
//			 if(JbpmConfiguration.getAnalyser(processName)!=null){
//				 if(activityName.equals("ALL"))
//					 JbpmConfiguration.getAnalyser(processName).printAllFutureSet();
//				 else
//					 JbpmConfiguration.getAnalyser(processName).printFutureSet(activityName);
//			 }
//		 }
		 response.sendRedirect("processes.jsp");
	}

}
