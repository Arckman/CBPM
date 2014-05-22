/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.ws.metadata.umdm;

// $Id: EndpointMetaData.java 4745 2007-10-12 08:18:30Z thomas.diesler@jboss.com $

import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Properties;
import java.util.Set;
import java.util.Observer;

import javax.jws.soap.SOAPBinding.ParameterStyle;
import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import javax.xml.ws.Service.Mode;

import org.jboss.logging.Logger;
import org.jboss.ws.Constants;
import org.jboss.ws.WSException;
import org.jboss.ws.core.CommonBindingProvider;
import org.jboss.ws.core.CommonSOAPBinding;
import org.jboss.ws.core.binding.TypeMappingImpl;
import org.jboss.ws.core.jaxrpc.binding.JBossXBDeserializerFactory;
import org.jboss.ws.core.jaxrpc.binding.JBossXBSerializerFactory;
import org.jboss.ws.core.jaxrpc.binding.SOAPArrayDeserializerFactory;
import org.jboss.ws.core.jaxrpc.binding.SOAPArraySerializerFactory;
import org.jboss.ws.core.jaxws.JAXBContextCache;
import org.jboss.ws.core.jaxws.JAXBDeserializerFactory;
import org.jboss.ws.core.jaxws.JAXBSerializerFactory;
import org.jboss.ws.core.jaxws.client.DispatchBinding;
import org.jboss.ws.core.soap.Style;
import org.jboss.ws.core.soap.Use;
import org.jboss.ws.metadata.config.CommonConfig;
import org.jboss.ws.metadata.config.Configurable;
import org.jboss.ws.metadata.config.ConfigurationProvider;
import org.jboss.ws.metadata.config.EndpointFeature;
import org.jboss.ws.metadata.config.JBossWSConfigFactory;
import org.jboss.wsf.common.JavaUtils;
import org.jboss.wsf.spi.binding.BindingCustomization;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedPortComponentRefMetaData;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedHandlerMetaData.HandlerType;

/**
 * A Service component describes a set of endpoints.
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-May-2005
 */
public abstract class EndpointMetaData extends ExtensibleMetaData implements ConfigurationProvider
{
   // provide logging
   private static Logger log = Logger.getLogger(EndpointMetaData.class);

   public enum Type
   {
      JAXRPC, JAXWS
   }

   public static final Set<String> SUPPORTED_BINDINGS = new HashSet<String>();
   static
   {
      SUPPORTED_BINDINGS.add(Constants.SOAP11HTTP_BINDING);
      SUPPORTED_BINDINGS.add(Constants.SOAP12HTTP_BINDING);
      SUPPORTED_BINDINGS.add(Constants.SOAP11HTTP_MTOM_BINDING);
      SUPPORTED_BINDINGS.add(Constants.SOAP12HTTP_MTOM_BINDING);
      SUPPORTED_BINDINGS.add(Constants.HTTP_BINDING);
   }

   // The parent meta data.
   private ServiceMetaData serviceMetaData;

   // The REQUIRED endpoint config
   private CommonConfig config;

   // The REQUIRED name
   private QName portName;
   // The REQUIRED binding id
   private String bindingId;
   // The REQUIRED name of the WSDL interface/portType
   private QName portTypeName;
   // The REQUIRED config-name
   protected String configName;
   // The REQUIRED config-file
   protected String configFile;
   // The endpoint interface name
   private String seiName;
   // The endpoint interface
   private Class seiClass;
   // The optional authentication method
   private String authMethod;
   // Arbitrary properties given by <call-property>
   private Properties properties;
   // The SOAPBinding style
   private Style style;
   // The SOAPBinding use
   private Use use;
   // The SOAPBinding parameter style
   private ParameterStyle parameterStyle;
   // The JAXWS ServiceMode
   private Mode serviceMode;
   // Whether the endpoint was deployed from annotations
   private Type type;
   // The list of service meta data
   private List<OperationMetaData> operations = new ArrayList<OperationMetaData>();
   // The optional handlers
   private List<HandlerMetaData> handlers = new ArrayList<HandlerMetaData>();
   // True if the handlers are initialized
   private boolean handlersInitialized;
   // Maps the java method to the operation meta data
   private Map<Method, OperationMetaData> opMetaDataCache = new HashMap<Method, OperationMetaData>();
   // All of the registered types
   private List<Class> registeredTypes = new ArrayList<Class>();

   private ConfigObservable configObservable = new ConfigObservable();

   private List<UnifiedPortComponentRefMetaData> serviceRefContrib = new ArrayList<UnifiedPortComponentRefMetaData>();

   private JAXBContextCache jaxbCache = new JAXBContextCache();

   private List<BindingCustomization> bindingCustomization = new ArrayList<BindingCustomization>();

   public EndpointMetaData(ServiceMetaData service, QName portName, QName portTypeName, Type type)
   {
      this.serviceMetaData = service;
      this.portName = portName;
      this.portTypeName = portTypeName;
      this.type = type;

      // The default binding
      this.bindingId = Constants.SOAP11HTTP_BINDING;
   }

   public ServiceMetaData getServiceMetaData()
   {
      return serviceMetaData;
   }

   public QName getPortName()
   {
      return portName;
   }

   public void setPortName(QName portName)
   {
      this.portName = portName;
   }

   public QName getPortTypeName()
   {
      return portTypeName;
   }

   public abstract String getEndpointAddress();

   public abstract void setEndpointAddress(String endpointAddress);

   public String getBindingId()
   {
      return bindingId;
   }

   public void setBindingId(String bindingId)
   {
      if (SUPPORTED_BINDINGS.contains(bindingId) == false)
         throw new WSException("Unsupported binding: " + bindingId);

      this.bindingId = bindingId;
   }

   public String getServiceEndpointInterfaceName()
   {
      return seiName;
   }

   public void setServiceEndpointInterfaceName(String seiName)
   {
      this.seiName = seiName;
      this.seiClass = null;

      UnifiedMetaData wsMetaData = serviceMetaData.getUnifiedMetaData();
      if (wsMetaData.isEagerInitialized())
      {
         if (UnifiedMetaData.isFinalRelease() == false)
            log.warn("Set SEI name after eager initialization", new IllegalStateException());

         // reinitialize
         initializeInternal();
      }
   }

   /** Get the class loader associated with the endpoint meta data */
   public ClassLoader getClassLoader()
   {
      return getServiceMetaData().getUnifiedMetaData().getClassLoader();
   }

   /**
    * Load the service endpoint interface.
    * It should only be cached during eager initialization.
    */
   public Class getServiceEndpointInterface()
   {
      Class tmpClass = seiClass;
      if (tmpClass == null && seiName != null)
      {
         try
         {
            ClassLoader classLoader = getClassLoader();
            tmpClass = classLoader.loadClass(seiName);
            if (serviceMetaData.getUnifiedMetaData().isEagerInitialized())
            {
               log.warn("Loading SEI after eager initialization");
               seiClass = tmpClass;
            }
         }
         catch (ClassNotFoundException ex)
         {
            throw new WSException("Cannot load service endpoint interface: " + seiName, ex);
         }
      }
      return tmpClass;
   }

   public Use getEncodingStyle()
   {
      if (use == null)
      {
         use = Use.getDefaultUse();
         log.debug("Using default encoding style: " + use);
      }
      return use;
   }

   public void setEncodingStyle(Use value)
   {
      if (value != null && use != null && !use.equals(value))
         throw new WSException("Mixed encoding styles not supported");

      log.trace("setEncodingStyle: " + value);
      this.use = value;
   }

   public Style getStyle()
   {
      if (style == null)
      {
         style = Style.getDefaultStyle();
         log.debug("Using default style: " + style);
      }
      return style;
   }

   public void setStyle(Style value)
   {
      if (value != null && style != null && !style.equals(value))
         throw new WSException("Mixed styles not supported");

      log.trace("setStyle: " + value);
      this.style = value;
   }

   public ParameterStyle getParameterStyle()
   {
      if (parameterStyle == null)
      {
         parameterStyle = ParameterStyle.WRAPPED;
         log.debug("Using default parameter style: " + parameterStyle);
      }
      return parameterStyle;
   }

   public void setParameterStyle(ParameterStyle value)
   {
      if (value != null && parameterStyle != null && !parameterStyle.equals(value))
         throw new WSException("Mixed SOAP parameter styles not supported");

      log.debug("setParameterStyle: " + value);
      this.parameterStyle = value;
   }

   public Mode getServiceMode()
   {
      return serviceMode;
   }

   public void setServiceMode(Mode serviceMode)
   {
      this.serviceMode = serviceMode;
   }

   public Type getType()
   {
      return type;
   }

   public Collection<BindingCustomization> getBindingCustomizations()
   {
      return bindingCustomization;
   }

   public String getAuthMethod()
   {
      return authMethod;
   }

   public void setAuthMethod(String authMethod)
   {
      this.authMethod = authMethod;
   }

   public Properties getProperties()
   {
      if (null == this.properties)
         this.properties = new Properties();
      return this.properties;
   }

   public void setProperties(Properties properties)
   {
      this.properties = properties;
   }

   public List<OperationMetaData> getOperations()
   {
      return new ArrayList<OperationMetaData>(operations);
   }

   public OperationMetaData getOperation(QName xmlName)
   {
      OperationMetaData opMetaData = null;
      for (OperationMetaData aux : operations)
      {
         QName opQName = aux.getQName();
         String javaName = aux.getJavaName();
         if (opQName.equals(xmlName) && !javaName.endsWith(Constants.ASYNC_METHOD_SUFFIX))
         {
            if (opMetaData == null)
            {
               opMetaData = aux;
            }
            else
            {
               throw new WSException("Cannot uniquely indentify operation: " + xmlName);
            }
         }
      }

      if (opMetaData == null && getStyle() == Style.DOCUMENT)
      {
         for (OperationMetaData auxOperation : operations)
         {
            ParameterMetaData paramMetaData = null;
            for (ParameterMetaData auxParam : auxOperation.getParameters())
            {
               ParameterMode mode = auxParam.getMode();
               if (!auxParam.isInHeader() && mode != ParameterMode.OUT)
               {
                  paramMetaData = auxParam;
                  break;
               }
            }
            if (paramMetaData != null && paramMetaData.getXmlName().equals(xmlName))
            {
               if (opMetaData == null)
               {
                  opMetaData = auxOperation;
               }
               else
               {
                  throw new WSException("Cannot uniquely indentify operation: " + xmlName);
               }
            }
         }
      }

      return opMetaData;
   }

   public OperationMetaData getOperation(Method method)
   {
      if (opMetaDataCache.size() == 0)
      {
         // This can happen when the SEI mapping was not found
         log.warn("Access to empty operation meta data cache, reinitializing");
         initializeInternal();
      }

      OperationMetaData opMetaData = opMetaDataCache.get(method);
      if (opMetaData == null)
      {
         for (OperationMetaData aux : operations)
         {
            boolean doesMatch = aux.getJavaMethod().equals(method);

            // fallback for async methods
            if (!doesMatch && method.getName().endsWith(Constants.ASYNC_METHOD_SUFFIX))
            {
               String name = method.getName();
               name = name.substring(0, name.length() - 5);
               doesMatch = aux.getJavaName().equals(name);
            }

            if (doesMatch)
            {
               opMetaDataCache.put(method, aux);
               opMetaData = aux;
               break;
            }

         }
      }
      return opMetaData;
   }

   public void addOperation(OperationMetaData opMetaData)
   {
      operations.add(opMetaData);
   }

   public void clearOperations()
   {
      operations.clear();
   }

   public void addHandlers(List<HandlerMetaData> configHandlers)
   {
      handlers.addAll(configHandlers);
   }

   public void addHandler(HandlerMetaData handler)
   {
      handler.setEndpointMetaData(this);
      handlers.add(handler);
   }

   public void clearHandlers()
   {
      handlers.clear();
      handlersInitialized = false;
   }

   public List<HandlerMetaData> getHandlerMetaData(HandlerType type)
   {
      List<HandlerMetaData> typeHandlers = new ArrayList<HandlerMetaData>();
      for (HandlerMetaData hmd : handlers)
      {
         if (hmd.getHandlerType() == type || type == HandlerType.ALL)
            typeHandlers.add(hmd);
      }
      return typeHandlers;
   }

   public boolean isHandlersInitialized()
   {
      return handlersInitialized;
   }

   public void setHandlersInitialized(boolean flag)
   {
      this.handlersInitialized = flag;
   }

   public void validate()
   {
      for (HandlerMetaData handler : handlers)
         handler.validate();

      for (OperationMetaData opMetaData : operations)
         opMetaData.validate();
   }

   /**
    * @see UnifiedMetaData#eagerInitialize()
    */
   public void eagerInitialize()
   {
      initializeInternal();
   }

   private void initializeInternal()
   {
      // reset sei class
      seiClass = null;

      // Initialize handlers
      for (HandlerMetaData handler : handlers)
         handler.eagerInitialize();

      eagerInitializeOperations();
      eagerInitializeTypes();
   }

   private void eagerInitializeOperations()
   {
      seiClass = getServiceEndpointInterface();
      if (seiClass != null)
      {
         List<Method> unsynchronizedMethods = new ArrayList<Method>();
         unsynchronizedMethods.addAll(Arrays.asList(seiClass.getMethods()));

         for (OperationMetaData opMetaData : operations)
         {
            opMetaData.eagerInitialize(unsynchronizedMethods);
            Method method = opMetaData.getJavaMethod();
            if (method != null)
            {
               opMetaDataCache.put(method, opMetaData);
               unsynchronizedMethods.remove(method);
            }
         }
      }
   }

   private void eagerInitializeTypes()
   {
      TypeMappingImpl typeMapping = serviceMetaData.getTypeMapping();
      List<TypeMappingMetaData> typeMappings = serviceMetaData.getTypesMetaData().getTypeMappings();
      registeredTypes = new ArrayList<Class>(typeMappings.size());
      for (TypeMappingMetaData tmMetaData : typeMappings)
      {
         String javaTypeName = tmMetaData.getJavaTypeName();
         QName xmlType = tmMetaData.getXmlType();
         if (xmlType != null)
         {
            List<Class> types = typeMapping.getJavaTypes(xmlType);

            // TODO: Clarification. In which cases is the type already registered?
            boolean registered = false;
            for (Class current : types)
            {
               if (current.getName().equals(javaTypeName))
               {
                  registeredTypes.add(current);
                  registered = true;
                  break;
               }
            }

            if (registered == false)
            {
               try
               {
                  ClassLoader classLoader = getClassLoader();
                  Class javaType = JavaUtils.loadJavaType(javaTypeName, classLoader);

                  if (JavaUtils.isPrimitive(javaTypeName))
                     javaType = JavaUtils.getWrapperType(javaType);

                  // Needed for runtime JAXB context
                  registeredTypes.add(javaType);

                  if (getEncodingStyle() == Use.ENCODED && javaType.isArray())
                  {
                     typeMapping.register(javaType, xmlType, new SOAPArraySerializerFactory(), new SOAPArrayDeserializerFactory());
                  }
                  else
                  {
                     if (getType() == Type.JAXWS)
                     {
                        typeMapping.register(javaType, xmlType, new JAXBSerializerFactory(), new JAXBDeserializerFactory());
                     }
                     else
                     {
                        typeMapping.register(javaType, xmlType, new JBossXBSerializerFactory(), new JBossXBDeserializerFactory());
                     }
                  }
               }
               catch (ClassNotFoundException e)
               {
                  log.warn("Cannot load class for type: " + xmlType + "," + javaTypeName);
               }
            }
         }
      }
   }

   // ---------------------------------------------------------------
   // Configuration provider impl

   /**
    * Callback for components that require configuration through jbossws-dd
    */
   public void configure(Configurable configurable)
   {
      // Make sure we have a configuration
      if (config == null)
         initEndpointConfig();

      // SOAPBinding configuration
      if (configurable instanceof CommonBindingProvider)
      {
         log.debug("Configure SOAPBinding");

         if (config.hasFeature(EndpointFeature.MTOM))
         {
            CommonBindingProvider provider = (CommonBindingProvider)configurable;
            ((CommonSOAPBinding)provider.getCommonBinding()).setMTOMEnabled(true);
            log.debug("Enable MTOM on endpoint " + getPortName());
         }
      }
      else if (configurable instanceof DispatchBinding)
      {
         DispatchBinding dpb = (DispatchBinding)configurable;
         dpb.setValidateDispatch(config.hasFeature(EndpointFeature.VALIDATE_DISPATCH));
      }
   }

   public UnifiedVirtualFile getRootFile()
   {
      return getServiceMetaData().getUnifiedMetaData().getRootFile();
   }

   public void registerConfigObserver(Configurable observer)
   {
      configObservable.addObserver(observer);
   }

   public JAXBContextCache getJaxbCache()
   {
      return jaxbCache;
   }

   public String getConfigFile()
   {
      return configFile;
   }

   public String getConfigName()
   {
      return configName;
   }

   public CommonConfig getConfig()
   {
      // Make sure we have a configuration
      if (config == null)
         initEndpointConfig();

      return config;
   }

   public void setConfigName(String configName)
   {
      setConfigNameInternal(configName, null);
   }

   public void setConfigName(String configName, String configFile)
   {
      setConfigNameInternal(configName, configFile);
   }

   private void setConfigNameInternal(String configName, String configFile)
   {
      if (configName == null)
         throw new IllegalArgumentException("Config name cannot be null");

      if (configFile != null)
         this.configFile = configFile;

      if (configName.equals(this.configName) == false)
      {
         this.configName = configName;

         log.debug("Reconfiguration forced, new config is '" + configName + "'");
         initEndpointConfig();
         configObservable.doNotify(configName);
      }
   }

   public void initEndpointConfig()
   {
      log.debug("Create new config [name=" + getConfigName() + ",file=" + getConfigFile() + "]");
      JBossWSConfigFactory factory = JBossWSConfigFactory.newInstance();
      config = factory.getConfig(getRootFile(), getConfigName(), getConfigFile());

      reconfigHandlerMetaData();
   }

   private void reconfigHandlerMetaData()
   {
      log.debug("Configure EndpointMetaData");

      List<HandlerMetaData> sepHandlers = getHandlerMetaData(HandlerType.ENDPOINT);
      clearHandlers();

      List<HandlerMetaData> preHandlers = config.getHandlers(this, HandlerType.PRE);
      List<HandlerMetaData> postHandlers = config.getHandlers(this, HandlerType.POST);

      addHandlers(preHandlers);
      addHandlers(sepHandlers);
      addHandlers(postHandlers);

      log.debug("Added " + preHandlers.size() + " PRE handlers");
      log.debug("Added " + sepHandlers.size() + " ENDPOINT handlers");
      log.debug("Added " + postHandlers.size() + " POST handlers");
   }

   public List<Class> getRegisteredTypes()
   {
      return Collections.unmodifiableList(registeredTypes);
   }

   class ConfigObservable extends Observable
   {

      private List<WeakReference<Observer>> observer = new ArrayList<WeakReference<Observer>>();

      public void doNotify(Object object)
      {
         setChanged();
         notifyObservers(object);
      }

      public synchronized void addObserver(Observer o)
      {
         observer.add( new WeakReference(o));
      }

      public synchronized void deleteObserver(Observer o)
      {
         for(WeakReference<Observer> w : observer)
         {
            Observer tmp = w.get();
            if(tmp.equals(o))
            {
               observer.remove(o);
               break;
            }

         }
      }

      public void notifyObservers()
      {
         notifyObservers(null);
      }

      public void notifyObservers(Object arg)
      {
         if(hasChanged())
         {
            for(WeakReference<Observer> w : observer)
            {
               Observer tmp = w.get();
               tmp.update(this, arg);

            }
         }
      }
   }

   public List<UnifiedPortComponentRefMetaData> getServiceRefContrib()
   {
      return serviceRefContrib;
   }

   public boolean matches(UnifiedPortComponentRefMetaData pcRef)
   {
      String seiName = pcRef.getServiceEndpointInterface();
      QName portName = pcRef.getPortQName();

      boolean match;
      if (seiName != null && portName != null)
      {
         match = getServiceEndpointInterfaceName().equals(seiName) && getPortName().equals(portName);
      }
      else
      {
         match = getServiceEndpointInterfaceName().equals(seiName) || getPortName().equals(portName);
      }
      return match;
   }
}
