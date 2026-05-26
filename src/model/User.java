package model;

public class User {

    private String fullName;
    private String phone;

    public User(String fullName, String phone) {
        this.fullName = fullName;
        this.phone    = phone;
    }

    public String getFullName() { return fullName; }

    // Alias so both getName() and getFullName() work everywhere
    public String getName()     { return fullName; }

    public String getPhone()    { return phone; }

    @Override
    public String toString() {
        return "User{name=" + fullName + ", phone=" + phone + "}";
    }
}
