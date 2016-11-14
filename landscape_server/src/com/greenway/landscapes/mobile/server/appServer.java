package com.greenway.landscapes.mobile.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.greenway.landscapes.common.UserObject;
import com.greenway.landscapes.common.dbConnection;
public class appServer {

	public static void main(String[] args) throws ClassNotFoundException, SQLException
	{
		try
		{
			ServerSocket myServerSocket = new ServerSocket(2597);
			System.out.println("appServer establish");
			for (;;)
			{
				Socket incoming = myServerSocket.accept();
				new ThreadClientHandler(incoming).start();
			}
			
		}
		catch (IOException e)
		{
			// When error occur, print the exception message.
			e.printStackTrace();
		}
	}
}

class ThreadClientHandler extends Thread {
	private Socket incoming;
	public ThreadClientHandler (Socket i)
	{
		incoming = i;
	}
	
	public void run()
	{
		try
		{
			boolean done = false;
			ObjectInputStream objectInputStream = new ObjectInputStream(incoming.getInputStream());
			ObjectOutputStream objectOutputStream = new ObjectOutputStream(incoming.getOutputStream());
			ResultSet rs;
			dbConnection dbconn = new dbConnection();

			//Login Process
			rs = dbconn.executeSQL("select aes_decrypt(user_password,'greenway') as pass, user_name, user_status from user;");
			UserObject user = (UserObject)objectInputStream.readObject();
			System.out.println("A client connected from " + incoming.getLocalAddress().getHostAddress());
			boolean found = false;
			String status = null;
			while(rs.next() && found != true)
			{
				if (user.getUsername().equals(rs.getString("user_name")) && user.getPassword().equals(rs.getString("pass")))
				{
					found = true;
					status = rs.getString("user_status");
				}
			}
			if (found)
			{
				int userStatus = -1;
				if (status.equals("Administrator"))
				{
					userStatus = 1;
				}
				else
				{
					userStatus = 2;
				}
				user.setStatus(userStatus);
				objectOutputStream.writeObject(user);
				objectOutputStream.flush();
				/*while(!done) {
					//Maintain connection for requests and processing
					user = (UserObject)objectInputStream.readObject();
					objectOutputStream.writeObject(user);
					objectOutputStream.flush();
				}*/
				System.out.println("it matches");
			}
			else
			{
				System.out.println("it doesn't matches");
			}
			// Close the connection.
			rs.close();
			objectInputStream.close();
			objectOutputStream.close();
			incoming.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

	}
}
