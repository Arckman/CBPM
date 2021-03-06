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
package javax.xml.soap;

/** A representation of a node (element) in an XML document. This interface
 * extnends the standard DOM Node interface with methods for getting and setting
 * the value of a node, for getting and setting the parent of a node, and for
 * removing a node
 * 
 * @author Scott.Stark@jboss.org
 * @version $Revision: 2897 $
 */
public interface Node extends org.w3c.dom.Node
{

   /**
    * Removes this Node object from the tree.
    */
   public void detachNode();

   /**
    * Returns the parent element of this Node object.
    * This method can throw an UnsupportedOperationException if the tree is not kept in memory.
    * @return the SOAPElement object that is the parent of this Node object or null if this Node object is root
    */
   public SOAPElement getParentElement();

   /**
    * Sets the parent of this Node object to the given SOAPElement object.
    * @param parent the SOAPElement object to be set as the parent of this Node object
    * @throws SOAPException  if there is a problem in setting the parent to the given element
    */
   public void setParentElement(SOAPElement parent) throws SOAPException;

   /**
    * Returns the value of this node if this is a Text node or the value of the immediate child of this node otherwise.
    * If there is an immediate child of this Node that it is a Text node then it's value will be returned.
    * If there is more than one Text node then the value of the first Text Node will be returned.
    * Otherwise null is returned.
    * @return a String with the text of this node if this is a Text node or the text contained by the first immediate
    * child of this Node object that is a Text object if such a child exists; null otherwise.
    */
   public String getValue();

   /**
    * If this is a Text node then this method will set its value, otherwise it sets the value of the immediate (Text)
    * child of this node. The value of the immediate child of this node can be set only if, there is one child node and
    * that node is a Text node, or if there are no children in which case a child Text node will be created.
    * @param value A value string
    * @throws IllegalStateException if the node is not a Text node and either has more than one child node or has a child node that is not a Text node.
    */
   public void setValue(String value);

   /**
    * Notifies the implementation that this Node object is no longer being used by the application and that the
    * implementation is free to reuse this object for nodes that may be created later.
    * Calling the method recycleNode implies that the method detachNode has been called previously.
    */
   public void recycleNode();
}
