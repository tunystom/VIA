package cz.via;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

//import com.mysql.jdbc.PreparedStatement;

import cz.via.domains.Message;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Servlet implementation class MainServlet
 */

@Path("/main")
public class MainServlet
{
    private static final long serialVersionUID = 1L;
    private static DataSource dS;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public MainServlet()
    {
        super();
        Context ctx;
        try
        {
            ctx = new InitialContext();
            dS = (DataSource) ctx.lookup("java:comp/env/jdbc/VIA");

        } catch (Exception e)
        {
            System.err.println("Cannot connect to DB with message: "
                    + e.getMessage());
            e.printStackTrace();
        }
    }

    @GET
    @Path("/")
    @Produces("application/json")
    public List<Message> getMessage() throws SQLException
    {            
        
        PreparedStatement preparedStatement = null;
        Connection c = null;
        try
        {
            c = dS.getConnection();
        } catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String select = "SELECT * FROM messages";
        try
        {
            preparedStatement = c.prepareStatement(select);
        } catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
        ResultSet rs = null;
        try
        {
            rs = preparedStatement.executeQuery();
        } catch (SQLException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        List<Message> messages = new ArrayList<Message>();
        while(rs.next()) {
            String first = rs.getString("name");
            String sec = rs.getString("message");
            Message mes = new Message();
            mes.setName(first);
            mes.setMessage(sec);
            messages.add(mes);
            
        }
        
        return messages;
    }

    @POST
    @Path("/")
    @Consumes("application/json")
    @Produces("text/html")
    public String storeMessage(Message message)
    {

        
        String insert = "INSERT INTO messages (name, message) VALUES (?,?)";

        java.sql.PreparedStatement preparedStatement = null;
        Connection dbConnection = null;
        try
        {
            dbConnection = dS.getConnection();
            String name = message.getName();
            String value = message.getMessage();
            preparedStatement = dbConnection.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, value);
            // execute insert SQL statement
            preparedStatement.executeUpdate();

            ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            int auto_id = rs.getInt(1);

        } catch (SQLException e)
        {
            return ("Insertion caused SQL exception  " + e.getMessage());
        } finally
        {

            if (preparedStatement != null)
            {
                try
                {
                    preparedStatement.close();
                } catch (SQLException e)
                {
                    return ("Error while closing prepared statement: " + e.getMessage());
                    
                }
            }

            if (dbConnection != null)
            {
                try
                {
                    dbConnection.close();
                } catch (SQLException e)
                {
                    return ("Error while closing db: " + e.getMessage());             
                    
                }
            }
        }
        return "OK";
    }

}
