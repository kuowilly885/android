package com.greenway.landscapes.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class dbConnection {
	
	final String JDBC_DRIVER = "com.mysql.jdbc.Driver";  
	final String DB_URL = "jdbc:mysql://localhost/greenway";
	public Connection conn = null;
	public Statement stmt = null;
	
	public dbConnection()
	{
		connectToDatabase("WebDev", "DevWeb");
	}
	
	public boolean connectToDatabase(String username, String password)
	{
	    try
	    {
	    	//STEP 2: Register JDBC driver
			Class.forName(JDBC_DRIVER);
		    //STEP 3: Open a connection
		    System.out.println("Connecting to database...");
		    conn = DriverManager.getConnection(DB_URL,username,password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	    return true;
	}
	
	public ResultSet executeSQL(String sql)
	{
		ResultSet result = null;
	    try
	    {
			stmt = conn.createStatement();
		    result = stmt.executeQuery(sql);
		}
	    catch (SQLException e)
	    {
			e.printStackTrace();
		}

	    return result;
	}

	public void REGISTER(String FirstName, String LastNamem, 
			String Status, String Email, String Phone, 
			String CreditCardNumber, String CreditCardSecureNumber,
			String CreditCardOwner, String CreditCardType, String BillAddress,
			String ExpireDate)
	{
		
	}
	
	public void TRANSACTION_QUERY(String FirstName, String LastName, String EmailAdress, String ProductContained)
	{
		String sql = "select cartid, tstatus, tdate"
				   + "from customer, credit_card, cart"
				   + "where customer.cid=credit_card.cid and cart.cardnum=credit_card.cardnumber"
				   		+ "and customer.fname=" + FirstName + " and customer.lname=" + LastName
				   		+ " and customer.email=" + EmailAdress;

	    try
	    {
			stmt = conn.createStatement();
		    stmt.executeUpdate(sql);
		}
	    catch (SQLException e)
	    {
			e.printStackTrace();
		}

	}
	

}
