package org.jboss.ws.extensions.eventing.mgmt;

/**
 * @author Heiko Braun, <heiko@openj.net>
 * @since 24-Jan-2006
 */
public final class SubscriptionError extends Exception {

   private String subcode;
   private String reason;

   public SubscriptionError(String subscode, String reason) {
      super();
      this.subcode = subscode;
      this.reason = reason;
   }

   public String getMessage() {
      return getReason();
   }

   public String getSubcode() {
      return subcode;
   }

   public String getReason() {
      return reason;
   }
      
}
