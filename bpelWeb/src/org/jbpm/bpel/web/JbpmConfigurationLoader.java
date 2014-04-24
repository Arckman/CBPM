/*    */ package org.jbpm.bpel.web;
/*    */ 
/*    */ import javax.servlet.ServletContext;
/*    */ import javax.servlet.ServletContextEvent;
/*    */ import javax.servlet.ServletContextListener;
/*    */ import org.jbpm.JbpmConfiguration;
/*    */ import org.jbpm.JbpmContext;
/*    */ import org.jbpm.svc.Services;
/*    */ 
/*    */ public class JbpmConfigurationLoader
/*    */   implements ServletContextListener
/*    */ {
/*    */   public void contextInitialized(ServletContextEvent event)
/*    */   {
/* 47 */     String resource = event.getServletContext().getInitParameter("jbpm.configuration.resource");
/* 48 */     JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(resource);
/*    */ 
/* 50 */     JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
/*    */     try {
/* 52 */       Services services = jbpmContext.getServices();
/* 53 */       services.getPersistenceService();
/* 54 */       services.getMessageService();
/* 55 */       services.getSchedulerService();
/* 56 */       services.getService("integration");
/*    */     }
/*    */     finally {
/* 59 */       jbpmContext.close();
/*    */     }
/*    */   }
/*    */ 
/*    */   public void contextDestroyed(ServletContextEvent event) {
/* 64 */     String resource = event.getServletContext().getInitParameter("jbpm.configuration.resource");
/* 65 */     JbpmConfiguration jbpmConfiguration = JbpmConfiguration.getInstance(resource);
/* 66 */     jbpmConfiguration.close();
/*    */   }
/*    */ }

/* Location:           C:\Users\frj\Desktop\bpel\engine codes\jbpm-bpel\jbpm-bpel-1.1.1\jbpm-bpel-1.1.1\deploy\jbpm-bpel\jbpm-bpel\WEB-INF\classes\
 * Qualified Name:     org.jbpm.bpel.web.JbpmConfigurationLoader
 * JD-Core Version:    0.6.0
 */