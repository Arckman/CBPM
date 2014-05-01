/*     */ package org.jbpm.bpel.web;
/*     */ 
/*     */ import java.io.File;
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.List;
/*     */ import java.util.regex.Pattern;
/*     */ import java.util.zip.ZipInputStream;
/*     */ import javax.servlet.ServletContext;
/*     */ import javax.servlet.ServletException;
/*     */ import javax.servlet.http.HttpServlet;
/*     */ import javax.servlet.http.HttpServletRequest;
/*     */ import javax.servlet.http.HttpServletResponse;
/*     */ import org.apache.commons.fileupload.FileItem;
/*     */ import org.apache.commons.fileupload.FileUploadException;
/*     */ import org.apache.commons.fileupload.disk.DiskFileItemFactory;
/*     */ import org.apache.commons.fileupload.servlet.ServletFileUpload;
/*     */ import org.apache.commons.logging.Log;
/*     */ import org.apache.commons.logging.LogFactory;
/*     */ import org.jbpm.JbpmConfiguration;
/*     */ import org.jbpm.JbpmContext;
/*     */ import org.jbpm.bpel.BpelException;
import org.jbpm.bpel.frj.VersionControlManager;
import org.jbpm.bpel.frj.interanalysis.mgr.Analyser;
/*     */ import org.jbpm.bpel.graph.def.BpelProcessDefinition;
/*     */ import org.jbpm.bpel.persistence.db.BpelGraphSession;
/*     */ import org.jbpm.bpel.tools.WebModuleBuilder;
/*     */ import org.jbpm.bpel.xml.ProblemHandler;
/*     */ import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.jpdl.par.ProcessArchive;
/*     */ 
/*     */ public class DeploymentServlet extends HttpServlet
/*     */ {
/*     */   private JbpmConfiguration jbpmConfiguration;
/*     */   private File deployDirectory;
/*     */   public static final String PARAM_DEPLOY_DIRECTORY = "deployDirectory";
/*     */   public static final String PARAM_PROCESS_ARCHIVE = "processArchive";
/*     */   private static final long serialVersionUID = 1L;
/*  62 */   private static final Pattern fileSeparatorPattern = compileFileSeparatorPattern();
/*     */ 
/*  64 */   private static final Log log = LogFactory.getLog(DeploymentServlet.class);
/*     */ 
/*     */   public void init() throws ServletException
/*     */   {
/*  68 */     String configResource = getServletContext().getInitParameter("configResource");
/*  69 */     this.jbpmConfiguration = JbpmConfiguration.getInstance(configResource);
/*     */ 
/*  72 */     String deployDirectoryName = getInitParameter("deployDirectory");
/*  73 */     if (deployDirectoryName == null) {
/*     */       String serverHomeDirectory;
/*     */       try {
/*  77 */         serverHomeDirectory = System.getProperty("jboss.server.home.dir");
/*     */       }
/*     */       catch (SecurityException e) {
/*  80 */         serverHomeDirectory = null;
/*     */       }
/*  82 */       if (serverHomeDirectory == null) {
/*  83 */         throw new ServletException("servlet parameter not found: deployDirectory");
/*     */       }
/*  85 */       deployDirectoryName = serverHomeDirectory + File.separatorChar + "deploy";
/*     */     }
/*     */ 
/*  89 */     this.deployDirectory = new File(deployDirectoryName);
/*  90 */     if (!this.deployDirectory.exists())
/*  91 */       throw new ServletException("deploy directory does not exist: " + this.deployDirectory);
/*     */   }
/*     */ 
/*     */   private static Pattern compileFileSeparatorPattern() {
/*  95 */     String expression = "[/\\\\]";
/*  96 */     if ((File.separatorChar != '/') && (File.separatorChar != '\\')) {
/*  97 */       expression = new StringBuffer(expression).insert(expression.length() - 2, File.separatorChar).toString();
/*     */     }
/*     */ 
/* 100 */     return Pattern.compile(expression);
/*     */   }
/*     */ 
/*     */   protected void doPost(HttpServletRequest request, HttpServletResponse response)
/*     */     throws ServletException, IOException
/*     */   {
/* 106 */     parseRequest(request);
/*     */ 
/* 108 */     FileItem fileItem = (FileItem)request.getAttribute("processArchive");
/* 109 */     String fileName = extractFileName(fileItem.getName());
/* 110 */     ProcessDefinition processDefinition = readProcessDefinition(fileItem.getInputStream(), fileName);
//============================================insert work this period================================================
			VersionControlManager vm=JbpmConfiguration.getVersionControlManager();
			if(vm.checkDynamicUpdatable()){
				//version consistent dynamic update
				vm.dynamicUpdate(processDefinition, fileName);
				if(vm.checkDynamicUpdatable())
	/* 111 */	synchronized(vm){
					try {
						vm.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
				deployProcessDefinition(processDefinition, fileName);
				String url="http://127.0.0.1:8080/";
				url+=fileName.split("\\.")[0]+"/";url+=processDefinition.getName()+"Provider";
				vm.addURL(processDefinition, url);
/*     */ 
/* 113 */     response.sendRedirect("processes.jsp");
/*     */   }
/*     */ 
/*     */   private void parseRequest(HttpServletRequest request) throws ServletException, IOException {
/* 117 */     if (!ServletFileUpload.isMultipartContent(request))
/* 118 */       throw new ServletException("request does not have multipart content");
/*     */     try
/*     */     {
/* 121 */       ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
/* 122 */       List items = upload.parseRequest(request);
/* 123 */       if (items.size() != 1) {
/* 124 */         throw new ServletException("deployment request must contain exactly one parameter");
/*     */       }
/* 126 */       request.setAttribute("processArchive", parseProcessArchive((FileItem)items.get(0)));
/*     */     }
/*     */     catch (FileUploadException e) {
/* 129 */       throw new ServletException("could not parse upload request", e);
/*     */     }
/*     */   }
/*     */ 
/*     */   private FileItem parseProcessArchive(FileItem processItem) throws ServletException {
/* 134 */     if (!"processArchive".equals(processItem.getFieldName())) {
/* 135 */       throw new ServletException("expected parameter 'processArchive', found: " + processItem.getFieldName());
/*     */     }
/*     */ 
/* 141 */     if (processItem.isFormField()) {
/* 142 */       throw new ServletException("parameter 'processArchive' is not an uploaded file");
/*     */     }
/*     */ 
/* 147 */     String contentType = processItem.getContentType();
/* 148 */     if ((!contentType.startsWith("application/zip")) && (!contentType.startsWith("application/x-zip-compressed")))
/*     */     {
/* 150 */       throw new ServletException("parameter 'processArchive' is expected to have content type 'application/zip' or 'application/x-zip-compressed', found: " + contentType);
/*     */     }
/*     */ 
/* 160 */     return processItem;
/*     */   }
/*     */ 
/*     */   private static String extractFileName(String filePath)
/*     */   {
/* 172 */     String[] fragments = fileSeparatorPattern.split(filePath);
/* 173 */     return fragments[(fragments.length - 1)];
/*     */   }
/*     */ 
/*     */   private ProcessDefinition readProcessDefinition(InputStream fileSource, String fileName) throws IOException
/*     */   {
/*     */     try {
/* 179 */       ProcessArchive processArchive = new ProcessArchive(new ZipInputStream(fileSource));
/* 180 */       log.debug("loaded process archive: " + fileName);
/*     */ 
/* 182 */       ProcessDefinition processDefinition = processArchive.parseProcessDefinition();
/* 183 */       log.debug("read process definition: " + processDefinition.getName());
/*     */ 
/* 185 */       ProcessDefinition localProcessDefinition1 = processDefinition;
/*     */       return localProcessDefinition1; } finally { fileSource.close(); }//localObject->NULL
/*     */   }
/*     */ 
/*     */   public void deployProcessDefinition(ProcessDefinition processDefinition, String fileName)
/*     */   {
/* 193 */     JbpmContext jbpmContext = this.jbpmConfiguration.createJbpmContext();
/*     */     try {
/* 195 */       if ((processDefinition instanceof BpelProcessDefinition)) {
/* 196 */         BpelProcessDefinition bpelProcessDefinition = (BpelProcessDefinition)processDefinition;
/* 197 */         BpelGraphSession.getContextInstance(jbpmContext).deployProcessDefinition(bpelProcessDefinition);
/*     */ 
/* 199 */         deployWebModule(bpelProcessDefinition, fileName);
/*     */       }
/*     */       else {
/* 202 */         jbpmContext.deployProcessDefinition(processDefinition);
/*     */       }
/* 204 */       log.info("deployed process definition: " + processDefinition.getName());
/*     */     }
/*     */     catch (RuntimeException e)
/*     */     {
/*     */       throw e;
/*     */     }
/*     */     finally {
/* 211 */       jbpmContext.close();
/*     */     }
/*     */   }
/*     */ 
/*     */   private void deployWebModule(BpelProcessDefinition processDefinition, String fileName) {
/* 216 */     File moduleFile = new File(this.deployDirectory, extractFilePrefix(fileName) + ".war");
/*     */ 
/* 218 */     WebModuleBuilder builder = new WebModuleBuilder();
/* 219 */     builder.setModuleFile(moduleFile);
/* 220 */     builder.buildModule(processDefinition);
/*     */ 
/* 222 */     if (builder.getProblemHandler().getProblemCount() > 0) {
/* 223 */       throw new BpelException("could not build web module for: " + processDefinition);
/*     */     }
/* 225 */     log.info("deployed web module: " + moduleFile.getName());
/*     */   }
/*     */ 
/*     */   private static String extractFilePrefix(String fileName) {
/* 229 */     int dotIndex = fileName.lastIndexOf('.');
/* 230 */     return dotIndex != -1 ? fileName.substring(0, dotIndex) : fileName;
/*     */   }
/*     */ }

/* Location:           C:\Users\frj\Desktop\bpel\engine codes\jbpm-bpel\jbpm-bpel-1.1.1\jbpm-bpel-1.1.1\deploy\jbpm-bpel\jbpm-bpel\WEB-INF\classes\
 * Qualified Name:     org.jbpm.bpel.web.DeploymentServlet
 * JD-Core Version:    0.6.0
 */