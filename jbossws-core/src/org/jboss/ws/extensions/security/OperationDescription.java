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
package org.jboss.ws.extensions.security;

import java.util.List;


/**
 * @author <a href="mailto:jason.greene@jboss.com">Jason T. Greene</a>
 * @version $Revision: 1757 $
 */
public class OperationDescription<T extends Operation>
{
   private Class<? extends T> operation;

   private List<Target> targets;

   private String certificateAlias;

   private String credential;

   private String algorithm;

   public OperationDescription(Class<? extends T> operation, List<Target> targets, String certicateAlias, String credential, String algorithm)
   {
      this.operation = operation;
      this.targets = targets;
      this.certificateAlias = certicateAlias;
      this.credential = credential;
      this.algorithm = algorithm;
   }

   public Class<? extends T> getOperation()
   {
      return operation;
   }

   public void setOperation(Class<? extends T> operation)
   {
      this.operation = operation;
   }

   public List<Target> getTargets()
   {
      return targets;
   }

   public void setTargets(List<Target> targets)
   {
      this.targets = targets;
   }


   public String getCertificateAlias()
   {
      return certificateAlias;
   }


   public void setCertificateAlias(String certificateAlias)
   {
      this.certificateAlias = certificateAlias;
   }


   public String getCredential()
   {
      return credential;
   }

   public void setCredential(String credential)
   {
      this.credential = credential;
   }

   public String getAlgorithm()
   {
      return algorithm;
   }

   public void setAlgorithm(String algorithm)
   {
      this.algorithm = algorithm;
   }

}
