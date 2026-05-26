package model;

import java.util.ArrayList;
import java.util.List;

public class Group {

    private int    id;
    private String groupName;
    private List<String> memberPhones;

    public Group(String groupName) {
        this.groupName    = groupName;
        this.memberPhones = new ArrayList<>();
    }

    public Group(int id, String groupName) {
        this.id           = id;
        this.groupName    = groupName;
        this.memberPhones = new ArrayList<>();
    }

    public int          getId()           { return id; }
    public String       getGroupName()    { return groupName; }
    public List<String> getMemberPhones() { return memberPhones; }

    public void setId(int id)              { this.id = id; }
    public void addMember(String phone)    { memberPhones.add(phone); }
    public boolean isMember(String phone)  { return memberPhones.contains(phone); }

    @Override
    public String toString() {
        return "Group{id=" + id + ", name=" + groupName + "}";
    }
}
