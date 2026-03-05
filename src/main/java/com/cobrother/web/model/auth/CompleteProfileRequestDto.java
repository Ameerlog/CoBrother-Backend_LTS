package com.cobrother.web.model.auth;

public class CompleteProfileRequestDto {

    private String firstname;
    private String lastname;

    public CompleteProfileRequestDto() {}

    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }

    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
}