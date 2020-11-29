package akkaHttp;

public class CacheMessage {
    public CacheMessage(String url, int time) {
        this.time = time;
        this.url = url;
    }

    private int time;
    private String url;

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
