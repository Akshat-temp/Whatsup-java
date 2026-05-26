package model;

public class Message {

    private int     id;
    private String  senderPhone;
    private String  receiverPhone;
    private int     groupId;
    private String  content;
    private String  timestamp;
    private boolean isDelivered;

   
    private String  mediaPath;
    private String  mediaType;  

    
    public Message(String senderPhone, String receiverPhone, String content, String timestamp) {
        this.senderPhone   = senderPhone;
        this.receiverPhone = receiverPhone;
        this.groupId       = 0;
        this.content       = content;
        this.timestamp     = timestamp;
        this.isDelivered   = false;
    }

    public Message(String senderPhone, int groupId, String content, String timestamp) {
        this.senderPhone   = senderPhone;
        this.receiverPhone = null;
        this.groupId       = groupId;
        this.content       = content;
        this.timestamp     = timestamp;
        this.isDelivered   = false;
    }

    public int     getId()            { return id; }
    public String  getSenderPhone()   { return senderPhone; }
    public String  getReceiverPhone() { return receiverPhone; }
    public int     getGroupId()       { return groupId; }
    public String  getContent()       { return content; }
    public String  getTimestamp()     { return timestamp; }
    public boolean isDelivered()      { return isDelivered; }
    public String  getMediaPath()     { return mediaPath; }
    public String  getMediaType()     { return mediaType; }

    public void setId(int id)                     { this.id = id; }
    public void setDelivered(boolean delivered)    { this.isDelivered = delivered; }
    public void setMediaPath(String mediaPath)     { this.mediaPath = mediaPath; }
    public void setMediaType(String mediaType)     { this.mediaType = mediaType; }

    public boolean isGroupMessage()  { return groupId != 0; }
    public boolean hasMedia()        { return mediaPath != null && !mediaPath.isEmpty(); }
}
