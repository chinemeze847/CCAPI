package core;

import javax.ws.rs.FormParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.core.MediaType;

@Path("sip")
public class SIP {

    @Context
    private UriInfo context;

    public SIP() {
    }

    @Path("main")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String main(@FormParam("isActive") String isActive, @FormParam("sessionId") String sessionId,
            @FormParam("callerNumber") String phoneNumber, @FormParam("callStartTime") String callStartTime,
            @FormParam("recordingUrl") String recordingUrl, @FormParam("durationInSeconds") int durationInSeconds,
            @FormParam("amount") double amount, @FormParam("destinationNumber") String dNumber,
            @FormParam("dtmfDigits") String inp) {
        
        System.out.println(sessionId+" SIP call received.");
        String response = "Cool";
        switch(isActive){
            case "1":
                response = "<Response>\n" +
                            "    <Dial phoneNumbers=\"+2348162156214\"/>\n" +
                            "</Response>";
                break;
            default:
                break;
        }
        return response;
    }
    
    @Path("check")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getText() {
        return "All is well";
    }
}
