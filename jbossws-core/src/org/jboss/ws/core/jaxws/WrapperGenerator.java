package org.jboss.ws.core.jaxws;

import org.jboss.ws.metadata.umdm.FaultMetaData;
import org.jboss.ws.metadata.umdm.ParameterMetaData;

public interface WrapperGenerator
{
   public void generate(ParameterMetaData pmd);
   public void generate(FaultMetaData fmd);
   public void reset(ClassLoader loader);
}