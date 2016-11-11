package com.greenway.landscapes.common;

import java.io.Serializable;

public class UserObject implements Serializable{
	String clientName;
	int userID;
	String username;
	String password;
	String[] products;
	int[] productID;
	int status;
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public UserObject() {
		
	}
	public String getClientName() {
		return clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	public int getUserID() {
		return userID;
	}
	public void setUserID(int userID) {
		this.userID = userID;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String[] getProducts() {
		return products;
	}
	public void setProducts(String[] products) {
		this.products = products;
	}
	public int[] getProductID() {
		return productID;
	}
	public void setProductID(int[] productID) {
		this.productID = productID;
	}

}
