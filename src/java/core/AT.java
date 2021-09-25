package core;

import static core.Router.dbConnect;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;

public final class AT {
    
    //<editor-fold defaultstate="collapsed" desc="Initiate Call">
    public static String makeCall(String phone, String staff) throws ClassNotFoundException{
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException ex) {
            Logger.getLogger(AT.class.getName()).log(Level.SEVERE, null, ex);
        }
        Unirest.config().verifySsl(false);
        HttpResponse<JsonNode> response = 
            Unirest.post("https://voice.africastalking.com/call")
                    .header("apiKey", "aa1a62005af48ca5fc83c8bb1e4865d60708a845c9e19e8b27f346823819e2be")            
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .field("username", "admire")
                    .field("from", "+23417006388")
                    .field("to", "+"+phone)
                    .asJson();
        
        JSONObject call_res = response.getBody().getObject();
        if(call_res.has("errorMessage")){
            String error = call_res.getString("errorMessage");
            if(error.equalsIgnoreCase("none")){
                JSONArray ja = call_res.getJSONArray("entries");
                JSONObject jo = ja.getJSONObject(0);
                if(jo.has("status")){
                    String status = jo.getString("status");
                    String sId = jo.getString("sessionId");
                    
                    try(Connection conn = dbConnect()){
                        String sql = "INSERT INTO fucalllog (phone,staff,status,sessionId) VALUES (?,?,?,?)";
                        PreparedStatement stmt = conn.prepareStatement(sql);
                        stmt.setString(1, phone);
                        stmt.setString(2, staff);
                        stmt.setString(3, status.toLowerCase());
                        stmt.setString(4, sId);
                        stmt.execute();
                    } catch (SQLException ex) {
                        Logger.getLogger(AT.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    return "success";
                }else{
                    return "failed";
                }
            }else{
                return "failed";
            }
        }else{
            return "failed";
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Send SMS">
    public static String sendSMS(String phone, String senderID, String message) throws ClassNotFoundException{
        Unirest.config().verifySsl(false);
        HttpResponse<JsonNode> response = 
            Unirest.post("https://api.africastalking.com/version1/messaging")
                    .header("apiKey", "aa1a62005af48ca5fc83c8bb1e4865d60708a845c9e19e8b27f346823819e2be")            
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Accept", "application/json")
                    .field("username", "admire")
                    .field("to", "+"+phone)
                    .field("message", message)
                    .field("from", senderID)
                    .asJson();
        
        JSONObject sms_res = response.getBody().getObject();
  
        if(sms_res.has("SMSMessageData")){
            JSONObject main_res = sms_res.getJSONObject("SMSMessageData");
            String atresponse = main_res.getString("Message");
            JSONArray ja = main_res.getJSONArray("Recipients");
            JSONObject jo = ja.getJSONObject(0);
            if(jo.has("status")){
                String status = jo.getString("status");
                String mId = jo.getString("messageId");
                String statusCode = jo.getString("statusCode");
                String cost = jo.getString("cost");

                try(Connection conn = dbConnect()){
                    String sql = "INSERT INTO atsmslog (phone,statuscode,status,messageId,message,cost,atresponse) "
                            + "VALUES (?,?,?,?,?,?,?)";
                    PreparedStatement stmt = conn.prepareStatement(sql);
                    stmt.setString(1, phone);
                    stmt.setString(2, statusCode);
                    stmt.setString(3, status);
                    stmt.setString(4, mId);
                    stmt.setString(5, message);
                    stmt.setString(6, cost);
                    stmt.setString(7, atresponse);                    
                    stmt.execute();
                } catch (SQLException ex) {
                    Logger.getLogger(AT.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(status.equalsIgnoreCase("success")){
                    return "success";
                }else{
                    return "failed";
                }
            }else{
                return "failed";
            }
        }else{
            return "failed";
        }
    }
    //</editor-fold>
}
