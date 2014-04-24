/*    */ package org.jbpm.bpel.web;
/*    */ 
/*    */ import java.io.IOException;
/*    */ import javax.servlet.ServletContext;
/*    */ import javax.servlet.ServletException;
/*    */ import javax.servlet.http.HttpServlet;
/*    */ import javax.servlet.http.HttpServletRequest;
/*    */ import javax.servlet.http.HttpServletResponse;
/*    */ import org.jbpm.JbpmConfiguration;
/*    */ 
/*    */ public class AdministrationServlet extends HttpServlet
/*    */ {
/*    */   private JbpmConfiguration jbpmConfiguration;
/*    */   public static final String PARAM_OPERATION = "operation";
/*    */   public static final String OP_CREATE_SCHEMA = "create_schema";
/*    */   public static final String OP_DROP_SCHEMA = "drop_schema";
/*    */   private static final long serialVersionUID = 1L;
/*    */ 
/*    */   public void init()
/*    */     throws ServletException
/*    */   {
/* 45 */     String configResource = getServletContext().getInitParameter("configResource");
/* 46 */     this.jbpmConfiguration = JbpmConfiguration.getInstance(configResource);
/*    */   }
/*    */ 
/*    */   protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
/*    */   {
/* 51 */     String operation = request.getParameter("operation");
/* 52 */     if ("create_schema".equals(operation)) {
/* 53 */       this.jbpmConfiguration.createSchema();
/* 54 */       log("created schema");
/*    */     }
/* 56 */     else if ("drop_schema".equals(operation)) {
/* 57 */       this.jbpmConfiguration.dropSchema();
/* 58 */       log("dropped schema");
/*    */     }
/*    */     else {
/* 61 */       throw new ServletException("value '" + operation + "' is not valid for parameter '" + "operation" + "'");
/*    */     }
/*    */ 
/* 67 */     response.sendRedirect("database.jsp");
/*    */   }
/*    */ }

/* Location:           C:\Users\frj\Desktop\bpel\engine codes\jbpm-bpel\jbpm-bpel-1.1.1\jbpm-bpel-1.1.1\deploy\jbpm-bpel\jbpm-bpel\WEB-INF\classes\
 * Qualified Name:     org.jbpm.bpel.web.AdministrationServlet
 * JD-Core Version:    0.6.0
 */