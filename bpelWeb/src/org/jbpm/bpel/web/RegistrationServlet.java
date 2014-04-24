/*     */ package org.jbpm.bpel.web;
/*     */ 
/*     */ import java.io.IOException;
/*     */ import java.io.InputStream;
/*     */ import java.util.List;
/*     */ import javax.servlet.ServletException;
/*     */ import javax.servlet.http.HttpServlet;
/*     */ import javax.servlet.http.HttpServletRequest;
/*     */ import javax.servlet.http.HttpServletResponse;
/*     */ import org.apache.commons.fileupload.FileItem;
/*     */ import org.apache.commons.fileupload.FileUploadException;
/*     */ import org.apache.commons.fileupload.disk.DiskFileItemFactory;
/*     */ import org.apache.commons.fileupload.servlet.ServletFileUpload;
/*     */ import org.jbpm.bpel.integration.catalog.CatalogEntry;
/*     */ import org.jbpm.bpel.integration.catalog.CentralCatalog;
/*     */ 
/*     */ public class RegistrationServlet extends HttpServlet
/*     */ {
/*     */   public static final String PARAM_BASE_LOCATION = "baseLocation";
/*     */   public static final String PARAM_DESCRIPTION_FILE = "descriptionFile";
/*     */   private static final long serialVersionUID = 1L;
/*     */ 
/*     */   protected void doPost(HttpServletRequest request, HttpServletResponse response)
/*     */     throws ServletException, IOException
/*     */   {
/*  49 */     parseRequest(request);
/*  50 */     registerDefinition(request);
/*  51 */     response.sendRedirect("partners.jsp");
/*     */   }
/*     */ 
/*     */   private void parseRequest(HttpServletRequest request) throws ServletException, IOException {
/*  55 */     if (!ServletFileUpload.isMultipartContent(request))
/*  56 */       throw new ServletException("request does not have multipart content");
/*     */     try
/*     */     {
/*  59 */       ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
/*  60 */       List items = upload.parseRequest(request);
/*  61 */       if (items.size() != 2) {
/*  62 */         throw new ServletException("registration request must contain two parameters");
/*     */       }
/*  64 */       request.setAttribute("baseLocation", parseBaseLocation((FileItem)items.get(0)));
/*  65 */       request.setAttribute("descriptionFile", parseDescriptionFile((FileItem)items.get(1)));
/*     */     }
/*     */     catch (FileUploadException e) {
/*  68 */       throw new ServletException("could not parse upload request", e);
/*     */     }
/*     */   }
/*     */ 
/*     */   private String parseBaseLocation(FileItem locationItem) throws ServletException {
/*  73 */     if (!"baseLocation".equals(locationItem.getFieldName())) {
/*  74 */       throw new ServletException("expected parameter 'baseLocation', found: " + locationItem.getFieldName());
/*     */     }
/*     */ 
/*  80 */     if (!locationItem.isFormField()) {
/*  81 */       throw new ServletException("parameter 'baseLocation' is not a simple form field");
/*     */     }
/*     */ 
/*  85 */     return locationItem.getString();
/*     */   }
/*     */ 
/*     */   private InputStream parseDescriptionFile(FileItem descriptionItem) throws ServletException, IOException
/*     */   {
/*  90 */     if (!"descriptionFile".equals(descriptionItem.getFieldName())) {
/*  91 */       throw new ServletException("expected parameter 'descriptionFile', found: " + descriptionItem.getFieldName());
/*     */     }
/*     */ 
/*  97 */     if (descriptionItem.isFormField()) {
/*  98 */       throw new ServletException("parameter 'descriptionFile' is not an uploaded file");
/*     */     }
/*     */ 
/* 103 */     if (descriptionItem.getSize() == 0L) {
/* 104 */       return null;
/*     */     }
/* 106 */     String contentType = descriptionItem.getContentType();
/* 107 */     if (!contentType.startsWith("text/xml")) {
/* 108 */       throw new ServletException("parameter 'descriptionFile' is expected to have content type 'text/xml', found: " + contentType);
/*     */     }
/*     */ 
/* 116 */     return descriptionItem.getInputStream();
/*     */   }
/*     */ 
/*     */   private void registerDefinition(HttpServletRequest request) throws IOException {
/* 120 */     String baseLocation = (String)request.getAttribute("baseLocation");
/* 121 */     InputStream descriptionSource = (InputStream)request.getAttribute("descriptionFile");
/*     */ 
/* 123 */     CentralCatalog catalog = CentralCatalog.getConfigurationInstance();
/* 124 */     catalog.addEntry(new CatalogEntry(baseLocation, descriptionSource));
/*     */   }
/*     */ }

/* Location:           C:\Users\frj\Desktop\bpel\engine codes\jbpm-bpel\jbpm-bpel-1.1.1\jbpm-bpel-1.1.1\deploy\jbpm-bpel\jbpm-bpel\WEB-INF\classes\
 * Qualified Name:     org.jbpm.bpel.web.RegistrationServlet
 * JD-Core Version:    0.6.0
 */