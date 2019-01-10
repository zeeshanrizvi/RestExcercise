package com.user;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.util.*;  
import javax.mail.*;  
import javax.mail.internet.*;  
import javax.activation.*;  
  
public class Service {
	
	// HashMap to store registered users
	static HashMap<String, String> map = new HashMap<>();
	
	static String ip = "localhost";
	static int port = 8073;
	static String smtpHost = "smtp.hpe.com";
	private static String smtpFromUserAddress = "zeeshan.riz.shaik@hpe.com";
	protected static String smtpFromUserPassword = "xxxx";

    public static void main(String[] args) throws IOException {  	
        // http web server and context creation
    	HttpServer server = HttpServer.create(new InetSocketAddress(Service.ip,Service.port), 0);
        HttpContext contextRegister = server.createContext("/Register");
        contextRegister.setHandler(Service::handleRegisterRequest);
        HttpContext contextLogin = server.createContext("/Login");
        contextLogin.setHandler(Service::handleLoginRequest);
        HttpContext contextVerifyEmail = server.createContext("/VerifyEmail");
        contextVerifyEmail.setHandler(arg0 -> {
			try {
				handleVerifyEmailRequest(arg0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
        // starting http web server
        server.start();      
    }
    
    
    // Method to handle POST /Register Requests
    // Http text/plain Json input will be stored into HashMap for further usage
    private static void handleRegisterRequest(HttpExchange exchange) throws IOException {
    	if (exchange.getRequestMethod().equals("POST")){
    		InputStream i = exchange.getRequestBody();
            String body = getStringFromInputStream(i);
            String UserId = body.split(",")[1].split(":")[1];
            UserId=UserId.substring(1, UserId.length()-1);
            Service.map.put(UserId, body);
            String response = "{'userid':'"+UserId+"' , 'state':'registered'}";
            exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();}
       }
    
    // Method to Handle Sign Up and sending http link to email Requests
    // On Request Dynamic /Login>* context will be generated and sent to User Id
    private static void handleVerifyEmailRequest(HttpExchange exchange) throws IOException, InterruptedException {
    	if (exchange.getRequestMethod().equals("PUT")){
    		InputStream i = exchange.getRequestBody();
            String body = getStringFromInputStream(i);
            String UserId = body.split(":")[1];
            UserId=UserId.substring(1, UserId.length()-2);
       
            // Verify Email Request time
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            Service.map.put(UserId,map.get(UserId)+"-"+timestamp.getTime());
      
            // SMTP Mail Procedure 
            Properties props = System.getProperties();  
            props.setProperty("mail.smtp.host",Service.smtpHost);
            props.setProperty("mail.smtp.starttls.enable","true");
            props.put("mail.smtp.auth", "true"); 
            //props.put("mail.smtp.port", "587");
    	    Session session = Session.getInstance(props,
    			  new javax.mail.Authenticator() {
    				protected PasswordAuthentication getPasswordAuthentication() {
    					return new PasswordAuthentication(Service.smtpFromUserAddress, Service.smtpFromUserPassword);
    				}
    			  });  
           //compose the message  
           try{  
    	          Message message = new MimeMessage(session);
			      message.setFrom(new InternetAddress(Service.smtpFromUserAddress));
			      message.setRecipients(Message.RecipientType.TO,
				  InternetAddress.parse(UserId));
			      message.setSubject("Mail Verification");
			      message.setText("http://"+Service.ip+":"+Service.port+"/Login?"+UserId);
                  // Send message  
                  Transport.send(message);
                  String response = "{'msg':'Mail sent to "+UserId+". Please click on link within 15 minutes to verify'}";
                  exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
                  OutputStream os = exchange.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
             }catch (MessagingException mex) {mex.printStackTrace();}
              finally {
                  String response = "{'msg':'Unable to process request right now.. please try again later..'}";
                  exchange.sendResponseHeaders(503, response.getBytes().length);//response code and length
                  OutputStream os = exchange.getResponseBody();
                  os.write(response.getBytes());
                  os.close();
             }
      
    	}
    }
    
    // Method to handle Login click from Email 
    // will output info of user from Hashmap if request is within 15 minutes 
    private static void handleLoginRequest(HttpExchange exchange) throws IOException {
        Timestamp timestampclick = new Timestamp(System.currentTimeMillis());
        String tUserId = exchange.getRequestURI().toString();
        tUserId = tUserId.substring(tUserId.indexOf("?")+1, tUserId.length()); 
        if (Service.map.containsKey(tUserId)){
             String RegTime = Service.map.get(tUserId).split("-")[1];
             long diff = Long.valueOf(timestampclick.getTime()) - Long.valueOf(RegTime);
             if (diff <= 900000){
                String response = "{'userInfo': "+Service.map.get(tUserId).split("-")[0]+",'msg':'Email Verified'}";
                exchange.sendResponseHeaders(200, response.getBytes().length);//response code and length
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                }
            else{
        	    String response = "{'msg':'link Expired'}";
                exchange.sendResponseHeaders(404, response.getBytes().length);//response code and length
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();	
             }
        }
             
         else {
            	 String response = "{'msg':'User Not Registered'}";
                 exchange.sendResponseHeaders(404, response.getBytes().length);//response code and length
                 OutputStream os = exchange.getResponseBody();
                 os.write(response.getBytes());
                 os.close();
             
             }
      }
        
        
    
    private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}
}
