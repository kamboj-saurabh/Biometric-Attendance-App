package com.syscode.attendance;

public class Attendance {
    public String reg;
    public  String ecode;
    public  String sheet_no;
    public  String room_name;
    public String status;

    public Attendance(String reg, String ecode, String sheet_no, String room_name, String status) {
        this.reg = reg;
        this.ecode = ecode;
        this.sheet_no = sheet_no;
        this.room_name = room_name;
        this.status = status;
    }

    public Attendance() {
    }

    public String getReg() {
        return reg;
    }

    public void setReg(String reg) {
        this.reg = reg;
    }

    public String getEcode() {
        return ecode;
    }

    public void setEcode(String ecode) {
        this.ecode = ecode;
    }

    public String getSheet_no() {
        return sheet_no;
    }

    public void setSheet_no(String sheet_no) {
        this.sheet_no = sheet_no;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toString(){
        return  "Sheet No: "+this.sheet_no+ "| Room No. :"+ this.room_name + "\n \n Reg.no. : "+ this.reg + " | E-code: " + this.ecode+"\n"+"Status:"+this.status;
    }
}
