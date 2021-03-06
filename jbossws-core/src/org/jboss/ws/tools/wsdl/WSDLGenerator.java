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
package org.jboss.ws.tools.wsdl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;

import org.apache.ws.policy.Policy;
import org.apache.ws.policy.util.PolicyFactory;
import org.apache.ws.policy.util.PolicyWriter;
import org.jboss.ws.Constants;
import org.jboss.ws.WSException;
import org.jboss.ws.core.soap.Style;
import org.jboss.ws.extensions.policy.PolicyScopeLevel;
import org.jboss.ws.extensions.policy.metadata.PolicyMetaExtension;
import org.jboss.ws.metadata.umdm.EndpointMetaData;
import org.jboss.ws.metadata.umdm.FaultMetaData;
import org.jboss.ws.metadata.umdm.MetaDataExtension;
import org.jboss.ws.metadata.umdm.OperationMetaData;
import org.jboss.ws.metadata.umdm.ParameterMetaData;
import org.jboss.ws.metadata.umdm.ServiceMetaData;
import org.jboss.ws.metadata.wsdl.Extendable;
import org.jboss.ws.metadata.wsdl.WSDLBinding;
import org.jboss.ws.metadata.wsdl.WSDLBindingFault;
import org.jboss.ws.metadata.wsdl.WSDLBindingOperation;
import org.jboss.ws.metadata.wsdl.WSDLBindingOperationInput;
import org.jboss.ws.metadata.wsdl.WSDLBindingOperationOutput;
import org.jboss.ws.metadata.wsdl.WSDLDefinitions;
import org.jboss.ws.metadata.wsdl.WSDLEndpoint;
import org.jboss.ws.metadata.wsdl.WSDLExtensibilityElement;
import org.jboss.ws.metadata.wsdl.WSDLImport;
import org.jboss.ws.metadata.wsdl.WSDLInterface;
import org.jboss.ws.metadata.wsdl.WSDLInterfaceFault;
import org.jboss.ws.metadata.wsdl.WSDLInterfaceOperation;
import org.jboss.ws.metadata.wsdl.WSDLInterfaceOperationInput;
import org.jboss.ws.metadata.wsdl.WSDLInterfaceOperationOutfault;
import org.jboss.ws.metadata.wsdl.WSDLInterfaceOperationOutput;
import org.jboss.ws.metadata.wsdl.WSDLProperty;
import org.jboss.ws.metadata.wsdl.WSDLRPCPart;
import org.jboss.ws.metadata.wsdl.WSDLRPCSignatureItem;
import org.jboss.ws.metadata.wsdl.WSDLSOAPHeader;
import org.jboss.ws.metadata.wsdl.WSDLService;
import org.jboss.ws.metadata.wsdl.WSDLRPCSignatureItem.Direction;
import org.jboss.wsf.common.DOMUtils;
import org.w3c.dom.Element;


/**
 * Generates a WSDL object model.
 *
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 3959 $
 */
public abstract class WSDLGenerator
{
   protected WSDLDefinitions wsdl;

   protected abstract void processTypes();

   protected void processEndpoint(WSDLService service, EndpointMetaData endpoint)
   {
      WSDLEndpoint wsdlEndpoint = new WSDLEndpoint(service, endpoint.getPortName());
      String address = endpoint.getEndpointAddress();
      wsdlEndpoint.setAddress(address == null ? "REPLACE_WITH_ACTUAL_URL" : address);
      service.addEndpoint(wsdlEndpoint);

      QName interfaceQName = endpoint.getPortTypeName();
      WSDLInterface wsdlInterface = new WSDLInterface(wsdl, interfaceQName);
      wsdl.addInterface(wsdlInterface);

      // Add imports
      if (!interfaceQName.getNamespaceURI().equals(endpoint.getServiceMetaData().getServiceName().getNamespaceURI()))
      {
         WSDLImport wsdlImport = new WSDLImport(wsdl);
         wsdlImport.setLocation(interfaceQName.getLocalPart() + "_PortType");
         wsdlImport.setNamespace(interfaceQName.getNamespaceURI());
         wsdl.addImport(wsdlImport);
         wsdl.registerNamespaceURI(interfaceQName.getNamespaceURI(), null);
      }

      QName bindingQName = new QName(interfaceQName.getNamespaceURI(), interfaceQName.getLocalPart() + "Binding");
      WSDLBinding wsdlBinding = new WSDLBinding(wsdl, bindingQName);
      wsdlBinding.setInterfaceName(interfaceQName);
      wsdl.addBinding(wsdlBinding);
      wsdlEndpoint.setBinding(bindingQName);

      for (OperationMetaData operation : endpoint.getOperations())
      {
         processOperation(wsdlInterface, wsdlBinding, operation);
      }
      
      //Policies
      MetaDataExtension ext = endpoint.getExtension(Constants.URI_WS_POLICY);
      if (ext != null)
      {
         PolicyMetaExtension policyExt = (PolicyMetaExtension)ext;
         for (Policy policy : policyExt.getPolicies(PolicyScopeLevel.WSDL_PORT))
         {
            addPolicyDefinition(policy);
            addPolicyReference(policy, wsdlEndpoint);
         }
         for (Policy policy : policyExt.getPolicies(PolicyScopeLevel.WSDL_PORT_TYPE))
         {
            addPolicyDefinition(policy);
            addPolicyURIAttribute(policy, wsdlInterface);
         }
         for (Policy policy : policyExt.getPolicies(PolicyScopeLevel.WSDL_BINDING))
         {
            addPolicyDefinition(policy);
            addPolicyReference(policy, wsdlBinding);
         }
      }
   }
   
   protected void addPolicyDefinition(Policy policy)
   {
      try
      {
         PolicyWriter writer = PolicyFactory.getPolicyWriter(PolicyFactory.StAX_POLICY_WRITER);
         ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
         writer.writePolicy(policy, outputStream);
         Element element = DOMUtils.parse(outputStream.toString(Constants.DEFAULT_XML_CHARSET));
         WSDLExtensibilityElement ext = new WSDLExtensibilityElement(Constants.WSDL_ELEMENT_POLICY, element);
         wsdl.addExtensibilityElement(ext);
         //optional: to obtain a better looking wsdl, register ws-policy
         //prefix in wsdl:definitions if it is not defined there yet
         if (wsdl.getPrefix(element.getNamespaceURI())==null)
         {
            wsdl.registerNamespaceURI(element.getNamespaceURI(),element.getPrefix());
         }
      }
      catch (IOException ioe)
      {
         throw new WSException("Error while converting policy to element!");
      }
   }
   
   protected void addPolicyReference(Policy policy, Extendable extendable)
   {
      QName policyRefQName = Constants.WSDL_ELEMENT_WSP_POLICYREFERENCE;
      String prefix = wsdl.getPrefix(policyRefQName.getNamespaceURI());
      if (prefix == null)
      {
         prefix = "wsp";
         wsdl.registerNamespaceURI(policyRefQName.getNamespaceURI(), prefix);
      }
      Element element = DOMUtils.createElement(policyRefQName.getLocalPart(), prefix);
      element.setAttribute("URI", policy.getPolicyURI());
      //TODO!! we need to understand if the policy is local or not...
      WSDLExtensibilityElement ext = new WSDLExtensibilityElement(Constants.WSDL_ELEMENT_POLICYREFERENCE, element);
      extendable.addExtensibilityElement(ext);
   }
   
   protected void addPolicyURIAttribute(Policy policy, Extendable extendable)
   {
      //TODO!! we need to understand if the policy is local or not...
      WSDLProperty prop = extendable.getProperty(Constants.WSDL_PROPERTY_POLICYURIS);
      if (prop == null)
      {
         extendable.addProperty(new WSDLProperty(Constants.WSDL_PROPERTY_POLICYURIS, policy.getPolicyURI()));
      }
      else
      {
         //PolicyURIs ships a comma separated list of URIs...
         prop.setValue(prop.getValue() + "," + policy.getPolicyURI());
      }
      
   }

   protected void processOperation(WSDLInterface wsdlInterface, WSDLBinding wsdlBinding, OperationMetaData operation)
   {
      WSDLInterfaceOperation interfaceOperation = new WSDLInterfaceOperation(wsdlInterface, operation.getQName());
      WSDLBindingOperation bindingOperation = new WSDLBindingOperation(wsdlBinding);

      interfaceOperation.setPattern(operation.isOneWay() ? Constants.WSDL20_PATTERN_IN_ONLY
            : Constants.WSDL20_PATTERN_IN_OUT);

      bindingOperation.setRef(operation.getQName());
      bindingOperation.setSOAPAction(operation.getSOAPAction());

      if (operation.getStyle() == Style.DOCUMENT)
         processOperationDOC(interfaceOperation, bindingOperation, operation);
      else
         processOperationRPC(interfaceOperation, bindingOperation, operation);

      for (FaultMetaData fault : operation.getFaults())
      {
         QName faultName = new QName(operation.getQName().getNamespaceURI(), fault.getXmlName().getLocalPart());
         WSDLInterfaceFault interfaceFault = new WSDLInterfaceFault(wsdlInterface, faultName);
         interfaceFault.setElement(fault.getXmlName());
         wsdlInterface.addFault(interfaceFault);
         
         WSDLInterfaceOperationOutfault outfault = new WSDLInterfaceOperationOutfault(interfaceOperation);
         outfault.setRef(faultName);
         interfaceOperation.addOutfault(outfault);

         WSDLBindingFault bindingFault = new WSDLBindingFault(wsdlBinding);
         bindingFault.setRef(faultName);
         wsdlBinding.addFault(bindingFault);
      }

      wsdlInterface.addOperation(interfaceOperation);
      wsdlBinding.addOperation(bindingOperation);
   }

   protected void addSignatureItem(WSDLInterfaceOperation operation, ParameterMetaData param, boolean isReturn)
   {
      Direction direction;
      if (isReturn)
      {
         direction = Direction.RETURN;
      }
      else if (param.getMode() == ParameterMode.INOUT)
      {
         direction = Direction.INOUT;
      }
      else if (param.getMode() == ParameterMode.OUT)
      {
         direction = Direction.OUT;
      }
      else
      {
         direction = Direction.IN;
      }

      operation.addRpcSignatureItem(new WSDLRPCSignatureItem(param.getPartName(), direction));
   }

   protected void processOperationDOC(WSDLInterfaceOperation interfaceOperation, WSDLBindingOperation bindingOperation, OperationMetaData operation)
   {
      interfaceOperation.setStyle(Constants.URI_STYLE_DOCUMENT);

      WSDLInterfaceOperationInput input = new WSDLInterfaceOperationInput(interfaceOperation);
      WSDLBindingOperationInput bindingInput = new WSDLBindingOperationInput(bindingOperation);

      WSDLInterfaceOperationOutput output = null;
      WSDLBindingOperationOutput bindingOutput = null;

      boolean twoWay = !operation.isOneWay();
      if (twoWay)
      {
         output = new WSDLInterfaceOperationOutput(interfaceOperation);
         bindingOutput = new WSDLBindingOperationOutput(bindingOperation);

         ParameterMetaData returnParameter = operation.getReturnParameter();
         if (returnParameter != null)
         {
            QName xmlName = returnParameter.getXmlName();
            String partName = returnParameter.getPartName();
            if (returnParameter.isInHeader())
            {
               WSDLSOAPHeader header = new WSDLSOAPHeader(xmlName, partName);
               header.setIncludeInSignature(true);
               bindingOutput.addSoapHeader(header);
            }
            else
            {
               output.setElement(xmlName);
               output.setPartName(partName);
            }
            addSignatureItem(interfaceOperation, returnParameter, true);
         }

         // If there is no return parameter, it will most likely be set later with an INOUT or OUT parameter.
         // Otherwise, a null element means there is a 0 body element part, which is allowed by BP 1.0
         interfaceOperation.addOutput(output);
         bindingOperation.addOutput(bindingOutput);
      }

      for (ParameterMetaData param : operation.getParameters())
      {
         if (param.isInHeader())
         {
            WSDLSOAPHeader header = new WSDLSOAPHeader(param.getXmlName(), param.getPartName());
            header.setIncludeInSignature(true);
            if (param.getMode() != ParameterMode.OUT)
               bindingInput.addSoapHeader(header);
            if (twoWay && param.getMode() != ParameterMode.IN)
               bindingOutput.addSoapHeader(header);
         }
         else
         {
            if (param.getMode() != ParameterMode.OUT)
            {
               input.setElement(param.getXmlName());
               input.setPartName(param.getPartName());
            }
            if (twoWay && param.getMode() != ParameterMode.IN)
            {
               output.setElement(param.getXmlName());
               output.setPartName(param.getPartName());
            }
         }
         addSignatureItem(interfaceOperation, param, false);
      }

      interfaceOperation.addInput(input);
      bindingOperation.addInput(bindingInput);
   }

   protected void processOperationRPC(WSDLInterfaceOperation interfaceOperation, WSDLBindingOperation bindingOperation, OperationMetaData operation)
   {
      interfaceOperation.setStyle(Constants.URI_STYLE_RPC);

      WSDLInterfaceOperationInput input = new WSDLInterfaceOperationInput(interfaceOperation);
      WSDLBindingOperationInput bindingInput = new WSDLBindingOperationInput(bindingOperation);
      QName operationName = operation.getQName();
      input.setElement(operationName);

      WSDLInterfaceOperationOutput output = null;
      WSDLBindingOperationOutput bindingOutput = null;

      boolean twoWay = !operation.isOneWay();
      if (twoWay)
      {
         output = new WSDLInterfaceOperationOutput(interfaceOperation);
         bindingOutput = new WSDLBindingOperationOutput(bindingOperation);
         output.setElement(new QName(operationName.getNamespaceURI(), operationName.getLocalPart() + "Response"));

         ParameterMetaData returnParameter = operation.getReturnParameter();
         if (returnParameter != null)
         {
            QName xmlName = returnParameter.getXmlName();
            String partName = returnParameter.getPartName();
            if (returnParameter.isInHeader())
            {
               WSDLSOAPHeader header = new WSDLSOAPHeader(xmlName, partName);
               header.setIncludeInSignature(true);
               bindingOutput.addSoapHeader(header);
            }
            else
            {
               WSDLRPCPart part = new WSDLRPCPart(returnParameter.getPartName(), returnParameter.getXmlType());
               output.addChildPart(part);
            }
            addSignatureItem(interfaceOperation, returnParameter, true);
         }

         interfaceOperation.addOutput(output);
         bindingOperation.addOutput(bindingOutput);
      }

      for (ParameterMetaData param : operation.getParameters())
      {
         if (param.isInHeader())
         {
            WSDLSOAPHeader header = new WSDLSOAPHeader(param.getXmlName(), param.getPartName());
            header.setIncludeInSignature(true);
            if (param.getMode() != ParameterMode.OUT)
               bindingInput.addSoapHeader(header);
            if (twoWay && param.getMode() != ParameterMode.IN)
               bindingOutput.addSoapHeader(header);
         }
         else
         {
            WSDLRPCPart part = new WSDLRPCPart(param.getPartName(), param.getXmlType());
            if (param.getMode() != ParameterMode.OUT)
               input.addChildPart(part);
            if (twoWay && param.getMode() != ParameterMode.IN)
               output.addChildPart(part);
         }
         addSignatureItem(interfaceOperation, param, false);
      }

      interfaceOperation.addInput(input);
      bindingOperation.addInput(bindingInput);
   }

   protected void processService(ServiceMetaData service)
   {

      WSDLService wsdlService = new WSDLService(wsdl, service.getServiceName());
      wsdl.addService(wsdlService);

      EndpointMetaData endpoint = null;
      for (Iterator<EndpointMetaData> iter = service.getEndpoints().iterator(); iter.hasNext();)
      {
         endpoint = iter.next();
         processEndpoint(wsdlService, endpoint);
      }

      if (endpoint == null)
         throw new IllegalStateException("A service must have an endpoint");

      wsdlService.setInterfaceName(endpoint.getPortName());
   }

   /**
    * Generate a WSDL object model from the passed in ServiceMetaData.
    *
    * @param service the service
    * @return the WSDL object model
    */
   public WSDLDefinitions generate(ServiceMetaData service)
   {
      // For now only WSDL 1.1
      wsdl = new WSDLDefinitions();
      wsdl.setWsdlNamespace(Constants.NS_WSDL11);

      // One WSDL per service
      String ns = service.getServiceName().getNamespaceURI();
      wsdl.setTargetNamespace(ns);
      wsdl.registerNamespaceURI(ns, "tns");
      wsdl.registerNamespaceURI(Constants.NS_SOAP11, "soap");
      wsdl.registerNamespaceURI(Constants.NS_SCHEMA_XSD, "xsd");

      processTypes();
      processService(service);

      return wsdl;
   }
}
