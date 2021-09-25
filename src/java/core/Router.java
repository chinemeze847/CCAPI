package core;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.FormParam;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

@Path("ccc")
public class Router {

    @Context
    private UriInfo context;

    public Router() {
    }
    
    @Path("landing")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String landing(@FormParam("isActive") String isActive, @FormParam("sessionId") String sessionId,
            @FormParam("callerNumber") String phoneNumber, @FormParam("callStartTime") String callStartTime,
            @FormParam("destinationNumber") String dNumber, @FormParam("dequeuedToPhoneNumber") String staff,
            @FormParam("direction") String direction, @FormParam("amount") String amount, 
            @FormParam("dequeueTime") String dTime, @FormParam("durationInSeconds") String duration,
            @FormParam("callerCountryCode") String cCCode, @FormParam("callerCarrierName") String network,
            @FormParam("recordingUrl") String recordingURL) 
            throws ClassNotFoundException {
        phoneNumber = phoneNumber.replace("+", "");
        String response;
        switch(isActive){
            case "1":
                if(checkCall(sessionId)){
                    //Specify call dequeue actions here
                    //Show a form (with fields to capture essential data) 
                    //to the staff with the customer's number that he's currently dequeuing
                    if(onQueue(sessionId)){
                        updateQueue(sessionId, staff);
                    }
                    System.out.println(staff+" responded to the customer's call.");
                }else if(checkFUCall(sessionId)){
                    String staffPhone = getInvolvedStaff(sessionId);
                    if(staffPhone.isEmpty()){
                        response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Response>"
                                + "<Reject />"
                            + "</Response>";
                    }else{
                        response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Response>"
                                + "<Dial\n" +
                                    "phoneNumbers=\"+"+staffPhone+"\"\n" +
                                    "ringbackTone=\"http://myomney.com/robomusic.mp3\"\n" +
                                    "record=\"true\" />"
                            + "</Response>";
                        updateFUCallInfo(sessionId);
                    }
                    logCallInfo(sessionId,phoneNumber,callStartTime,direction,cCCode);
                    return response;
                }
                String is_a[] = isStaff(phoneNumber).split("_");
                if(Boolean.valueOf(is_a[0])){ 
                    String unit = is_a[2];
                    switch(is_a[1]){
                        case "internal":
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                +   "<Dequeue name=\""+unit+"\" phoneNumber=\"+23417006388\" />"
                                + "</Response>";
                            break; 
                        case "external":
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<GetDigits timeout=\"25\" finishOnKey=\"#\" callbackUrl=\"http://167.172.54.146:8080/CCAPI/ccc/followup\">"
                                        +"<Say>Please enter destination phone number, followed by # key."
                                        +"</Say>"
                                    + "</GetDigits>" 
                                + "</Response>";
                            break;
                        default:
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Reject />" 
                                + "</Response>";
                            break;
                    }
                }else{
                    String greeting = getGreeting();
                    if(checkCustomer(phoneNumber)){
                        response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Response>"
                                    + "<GetDigits timeout=\"30\" finishOnKey=\"#\" callbackUrl=\"http://167.172.54.146:8080/CCAPI/ccc/known\">"
                                        +"<Say>Hello..."+getGreeting()+"Welcome to Admire. How would you like us to serve you today? "
                                            + "To report a fraud, press one followed by the # key "
                                            + "For Akawo Cash Pick Up, Press Two followed by the # key "
                                            + "For BVN, press three followed by the # key "
                                            + "For Cash Withdrawal, Press Four followed by the # key "
                                            + "For Deposit to Banks, Press Five followed by the # key "
                                            + "To speak with Customer Care, Press Zero followed by the # key"
                                        +"</Say>"
                                    + "</GetDigits>"
                                    + "<GetDigits timeout=\"10\" numDigits=\"1\" finishOnKey=\"#\" callbackUrl=\"http://167.172.54.146:8080/CCAPI/ccc/known\">"
                                        +"<Say>You have not entered any response, please give us a call back when you are "
                                            + "ready."
                                        +"</Say>"
                                    + "</GetDigits>"
                            + "</Response>";
                    }else{
                        response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Response>"
                                + "<GetDigits timeout=\"30\" numDigits=\"1\" finishOnKey=\"#\" callbackUrl=\"http://167.172.54.146:8080/CCAPI/ccc/unknown\">"
                                    +"<Say>Hello..."+greeting+" This is Admire. Do you know that with Admire, you can chat with "
                                        + "anyone, anywhere; save money in a way that is convenient to you; send and receive "
                                        + "money, resolve BVN issues and so much more."
                                        + "Press 1 to begin. "
                                        + "Press Zero to speak with customer care."
                                    +"</Say>"
                                + "</GetDigits>"
                                + "<GetDigits timeout=\"10\" numDigits=\"1\" finishOnKey=\"#\" callbackUrl=\"http://167.172.54.146:8080/CCAPI/ccc/unknown\">"
                                    +"<Say>You have not entered any response, please give us a call back when you are "
                                        + "ready."
                                    +"</Say>"
                                + "</GetDigits>"
                            + "</Response>";
                    }
                }
                logCallInfo(sessionId,phoneNumber,callStartTime,direction,cCCode);
                break;
            default:
                if(staff!=null){
                    updateCallInfo(sessionId,staff,duration,amount,dTime,network);
                }else{
                    updateCallInfo(sessionId,duration,amount,network);
                }
                if(checkFUCall(sessionId)){
                    updateFUCallInfo(sessionId,recordingURL,timeNow(),Integer.parseInt(duration));
                }
                response = "Final leg";
                break;
        }
        
        return response;
    }

    @Path("followup")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String followUp(@FormParam("isActive") String isActive, @FormParam("sessionId") String sessionId,
            @FormParam("callerNumber") String phoneNumber, @FormParam("callStartTime") String callStartTime,
            @FormParam("recordingUrl") String recordingUrl, @FormParam("durationInSeconds") String duration,
            @FormParam("amount") String amount, @FormParam("destinationNumber") String dNumber,
            @FormParam("dtmfDigits") String inp, @FormParam("callerCarrierName") String network) throws ClassNotFoundException {
        final String staffPhone = phoneNumber.replace("+", "");
        String desPhone;
        String response;
        switch(isActive){
            case "1":
                response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<Response>"
                                +"<Reject />"
                            + "</Response>";
                if(inp!=null){
                    desPhone = "234"+inp.substring(1);
                }else{
                    return response;
                }
                new Thread(() -> {
                    try {
                        AT.makeCall(desPhone, staffPhone);
                    } catch (ClassNotFoundException e) {
                        Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, e);
                    }
                }).start();
                
                break;
            default:
                updateCallInfo(sessionId,duration,amount,network);
                response = "Final leg";
                break;
        }
        return response;
    }
    
    @Path("known")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String known(@FormParam("isActive") String isActive, @FormParam("sessionId") String sessionId,
            @FormParam("callerNumber") String phoneNumber, @FormParam("callStartTime") String callStartTime,
            @FormParam("recordingUrl") String recordingUrl, @FormParam("durationInSeconds") String duration,
            @FormParam("amount") String amount, @FormParam("destinationNumber") String dNumber,
            @FormParam("dtmfDigits") String inp, @FormParam("callerCarrierName") String network,
            @FormParam("dialDestinationNumber") String staff, @FormParam("dialStartTime") String dTime,
            @FormParam("dequeuedToPhoneNumber") String qStaff)
            throws ClassNotFoundException {
        phoneNumber = phoneNumber.replace("+", "");
        String response, request;
        switch(isActive){
            case "1":
                if(onQueue(sessionId)){
                    updateQueue(sessionId, qStaff);
                    System.out.println(qStaff+" responded to the customer's call.");
                    response = "What are you expecting to see?";
                }else{
                    switch (inp){
                        case "0":
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Enqueue name=\"backend\" holdMusic=\"http://myomney.com/robomusic.mp3\" />"
                                + "</Response>";
                            addToQueue(phoneNumber, sessionId, "backend");
                            break;
                        case "1":
                            //fraud
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Dial\n" +
                                        "phoneNumbers=\"+2347064749857,+2348162156214\"\n" +
                                        "ringbackTone=\"http://myomney.com/robomusic.mp3\"\n" +
                                        "record=\"true\" />"
                                + "</Response>";
                            logRequest(phoneNumber, "fraud", "new");
                            break;
                        case "2":
                            //akawo
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>Your request has been noted. We will process it and get back to "
                                    + "you shortly. Thank you.</Say>"
                                + "</Response>";
                            logRequest(phoneNumber, "akawo", "new");
                            break;
                        case "3":
                            //bvn
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>Your request has been noted. We will process it and get back to "
                                    + "you shortly. Thank you.</Say>"
                                + "</Response>";
                            logRequest(phoneNumber, "bvn", "new");
                            break;
                        case "4":
                            //withdrawal
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>This feature will be activated soon. We'll notify you when we're set. Thank you.</Say>"
                                + "</Response>";
                            logRequest(phoneNumber, "withdrawal", "new");
                            break;
                        case "5":
                            //deposit
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>This feature will be activated soon. We'll notify you when we're set. Thank you.</Say>"
                                + "</Response>";
                            logRequest(phoneNumber, "deposit", "new");
                            break;
                        default:
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>Your response could not be understood. Please try again.</Say>"
                                + "</Response>";
                    }
                }
                break;
            default:
                if(staff!=null){
                    updateCallInfo(sessionId,staff,duration,amount,dTime,network);
                }else{
                    updateCallInfo(sessionId,duration,amount,network);
                }
                response = "Final leg from known customer";
                break;
        }
        return response;
    }
    
    @Path("unknown")
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String unknown(@FormParam("isActive") String isActive, @FormParam("sessionId") String sessionId,
            @FormParam("callerNumber") String phoneNumber, @FormParam("callStartTime") String callStartTime,
            @FormParam("recordingUrl") String recordingUrl, @FormParam("durationInSeconds") String duration,
            @FormParam("amount") String amount, @FormParam("destinationNumber") String dNumber,
            @FormParam("dtmfDigits") String inp, @FormParam("callerCarrierName") String network,
            @FormParam("dequeuedToPhoneNumber") String qStaff) 
            throws ClassNotFoundException {
        phoneNumber = phoneNumber.replace("+", "");
        String response;
        switch(isActive){
            case "1":
                if(onQueue(sessionId)){
                    updateQueue(sessionId, qStaff);
                    System.out.println(qStaff+" responded to the customer's call.");
                    response = "What are you expecting to see?";
                }else{
                    switch(inp){
                        case "0":
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Enqueue name=\"frontend\" holdMusic=\"http://myomney.com/robomusic.mp3\" />"
                                + "</Response>";
                            addToQueue(phoneNumber, sessionId, "frontend");
                            break;
                        default:
                            response = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + "<Response>"
                                    + "<Say>Your request has been noted. We will process it and get back to "
                                    + "you shortly.</Say>"
                                + "</Response>";
                            logRequest(phoneNumber, "enquiry", "new");
                            break;
                    }
                }
                break;
            default:
                updateCallInfo(sessionId,duration,amount,network);
                response = "Final leg from unknown menu";
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
    
    public static Connection dbConnect() throws ClassNotFoundException, SQLException{
        String URL = "jdbc:mysql://10.106.0.2:3306/admirecc?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Africa/Lagos";
        String USERNAME = "webapp";
        String PASSWORD = "Ilmm56;;";
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
    
    private boolean checkTrackingID(String ID) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM request WHERE trackingID = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, ID);
            ResultSet rs = stmt.executeQuery();
            
            return rs.next();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private String isStaff(String phoneNumber) throws ClassNotFoundException {
        String res = "false";
        
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM staff WHERE phone = ? AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phoneNumber);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                StringBuilder resb = new StringBuilder("true_");
                resb.append(rs.getString("role"));
                resb.append("_");
                resb.append(rs.getString("unit"));
                res = resb.toString();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
        return res;
    }

    private boolean checkCustomer(String phoneNumber) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM customer WHERE phone = ? AND status = 'active'";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phoneNumber);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    private void updateCallInfo(String sessionId, String staff, String duration, 
            String amount, String dTime, String network) throws ClassNotFoundException {
        //sessionId,staff,duration,amount,dTime,network
        try(Connection conn = dbConnect()){
            String sql = "UPDATE pokelog SET staff = ?, duration = ?, amount = ?, dTime = ?, network = ? "
                    + "WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, staff);
            stmt.setInt(2, Integer.valueOf(duration));
            stmt.setDouble(3, Double.valueOf(amount));
            stmt.setString(4, dTime);
            stmt.setString(5, network);
            stmt.setString(6, sessionId);
            stmt.execute();
            
            if(onQueue(sessionId)){
                updateQueue(sessionId);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void logCallInfo(String sessionId, String phoneNumber, String callStartTime, 
            String direction, String ccc) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "INSERT INTO pokelog (sessionId,phoneNumber,direction,callerCountryCode,callStartTime) "
                    + "VALUES(?,?,?,?,?);";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionId);
            stmt.setString(2, phoneNumber);
            stmt.setString(3, direction);
            stmt.setString(4, ccc);
            stmt.setString(5, callStartTime );
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean checkCall(String sessionId) throws ClassNotFoundException{
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM pokelog WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }
    
    private void updateCallInfo(String sessionId, String duration, String amount, String network) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "UPDATE pokelog SET duration = ?, amount = ?, network = ? "
                    + "WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, Integer.valueOf(duration));
            stmt.setDouble(2, Double.valueOf(amount));
            stmt.setString(3, network);
            stmt.setString(4, sessionId);
            stmt.execute();
            
            if(onQueue(sessionId)){
                updateQueue(sessionId);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getGreeting() {
        String greeting;
        ZoneId b = ZoneId.of("Africa/Lagos");
        int timeH = LocalDateTime.now(b).getHour();
        if(timeH<12){
            greeting = " Good Morning ";
        }else if(timeH<17 && timeH>=12){
            greeting = " Good Afternoon ";
        }else{
            greeting = " Good Evening ";
        }
        return greeting;
    }

    private void logInquiry(String phone, String status) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "INSERT INTO inquiry (phone, status) VALUES (?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, phone);
            stmt.setString(2, status);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void logRequest(String phone, String type, String status) throws ClassNotFoundException {
        String trackingID;
        String watcher = "match";
        do {
            trackingID = "TRK"+randomCode(5);
            if (!checkTrackingID(trackingID))watcher="nomatch";
        } while (watcher.equals("match"));
        try(Connection conn = dbConnect()){
            String sql = "INSERT INTO request (trackingID, phone, type, status, assignedTo) VALUES (?,?,?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, trackingID);
            stmt.setString(2, phone);
            stmt.setString(3, type);
            stmt.setString(4, status);
            stmt.setString(5, "");
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String randomCode(int len){
        SecureRandom rnd = new SecureRandom();
        String AN = "ABCDEFGHIJKLMNPQRSTUVWXYZ123456789";
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append(AN.charAt(rnd.nextInt(AN.length())));
        return sb.toString();
    }
    
    //<editor-fold defaultstate="collapsed" desc="Vet Session ID 4 FU Call">
    private boolean checkFUCall(String sessionId) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM fucalllog WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }
    //</editor-fold>

    private String getInvolvedStaff(String sessionId) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM fucalllog WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sessionId);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return rs.getString("staff");
            }
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
        return "";
    }
    
    //<editor-fold defaultstate="collapsed" desc="timeNow">
    String timeNow(){
        Date dw1 = new Date();
        SimpleDateFormat ft = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        ft.setTimeZone(TimeZone.getTimeZone("Africa/Lagos"));
        String dw = (String) ft.format(dw1);
        
        return dw;
    }
    //</editor-fold>

    private void updateFUCallInfo(String sessionId, String recordingURL, String timeNow, int duration) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "UPDATE fucalllog SET status = ?, recordingURL = ?, doneTime = ? , duration = ? "
                    + "WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "done");
            stmt.setString(2, recordingURL);
            stmt.setString(3, timeNow);
            stmt.setInt(4, duration);
            stmt.setString(5, sessionId);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateFUCallInfo(String sessionId) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "UPDATE fucalllog SET status = ? WHERE sessionId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, "dequeued");
            stmt.setString(2, sessionId);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void addToQueue(String phone, String sId, String mode) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql = "INSERT INTO queue (sId, phone, mode) VALUES (?,?,?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sId);
            stmt.setString(2, phone);
            stmt.setString(3, mode);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private boolean onQueue(String sId) throws ClassNotFoundException{
        try(Connection conn = dbConnect()){
            String sql = "SELECT * FROM queue WHERE sId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, sId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    private void updateQueue(String sId, String staff) throws ClassNotFoundException {
        staff = staff.replace("+", "");
        try(Connection conn = dbConnect()){
            String sql = "UPDATE queue SET status = 'oncall', staff = ?, dqTime = ? WHERE sId = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, staff);
            stmt.setString(2, timeNow());
            stmt.setString(3, sId);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void updateQueue(String sId) throws ClassNotFoundException {
        try(Connection conn = dbConnect()){
            String sql;
            PreparedStatement stmt;
            
            sql = "SELECT * FROM queue WHERE status = 'oncall' AND sId = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sId);
            ResultSet rs = stmt.executeQuery();
            
            if(rs.next()){
                sql = "UPDATE queue SET status = 'done' WHERE sId = ?";
            }else{
                sql = "UPDATE queue SET status = 'abandoned' WHERE sId = ?";
            }
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sId);
            stmt.execute();
        } catch (SQLException ex) {
            Logger.getLogger(Router.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
