package com.syscode.attendance;
import com.google.firebase.database.IgnoreExtraProperties;


import java.util.ArrayList;

/**
 *
 */

@IgnoreExtraProperties
public class User {

    public String name;
    public  String student_id,fingerprint1,ecode,reg;
    public  String room_name;

    ArrayList<String> list = new ArrayList<String>();
    public User(String name,String room_name, String fingerprint1, String reg, String ecode, String student_id) {
        this.name = name;
        this.student_id = student_id;
        this.fingerprint1 = fingerprint1;
        this.room_name = room_name;
        this.reg = reg;
        this.ecode = ecode;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEcode() {
        return ecode;
    }

    public void setEcode(String ecode) {
        this.ecode = ecode;
    }

    public String getReg() {
        return reg;
    }

    public void setReg(String reg) {
        this.reg = reg;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public String getStudent_id() {
        return student_id;
    }

    public void setStudent_id(String student_id) {
        this.student_id = student_id;
    }


    public void setFingerprint1(String fingerprint1) {
        this.fingerprint1 = fingerprint1;
    }

    public String getFingerprint1() {
        return fingerprint1;
    }

    public String toString(){
        return  "Name: "+this.name+ "| Room No. :"+ this.room_name + "\n \n Reg.no. : "+ this.reg + " | E-code: " + this.ecode+"\n";
    }


}