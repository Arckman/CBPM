package org.jboss.ws.extensions.policy.metadata;


import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.ws.policy.Policy;
import org.jboss.ws.extensions.policy.PolicyScopeLevel;
import org.jboss.ws.metadata.umdm.MetaDataExtension;

public class PolicyMetaExtension extends MetaDataExtension
{
   //Policies may be attached to a policy subject with different policy scopes
   private Map<PolicyScopeLevel,Collection<Policy>> policies = new HashMap<PolicyScopeLevel,Collection<Policy>>();
   
   public PolicyMetaExtension(String extensionNameSpace)
   {
      super(extensionNameSpace);
   }
   
   public void addPolicy(PolicyScopeLevel scope, Policy policy)
   {
      Collection<Policy> list;
      if (!policies.containsKey(scope))
      {
         list = new LinkedList<Policy>();
         policies.put(scope,list);
      }
      else
      {
         list = policies.get(scope);
      }
      list.add(policy);
   }
   
   public Collection<Policy> getPolicies(PolicyScopeLevel scope)
   {
      Collection<Policy> policyCollection = policies.get(scope);
      return policyCollection == null ? new LinkedList<Policy>() : policyCollection;
   }
   
   public Collection<Policy> getAllPolicies()
   {
      Collection<Policy> list = new LinkedList<Policy>();
      for (PolicyScopeLevel scope : policies.keySet())
      {
         list.addAll(policies.get(scope));
      }
      return list;
   }
   
}
