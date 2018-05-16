package data;

import org.apache.commons.net.util.Base64;

import java.nio.charset.StandardCharsets;

public class ChangePair {
    String filename;
    private String src;
    private String dst;

    public ChangePair(String filename, String src, String dst) {
        this.filename = filename;
        this.src = src;
        this.dst = dst;
    }

    public ChangePair(String src, String dst) {
        this.src = src;
        this.dst = dst;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getDst() {
        return dst;
    }

    public void setDst(String dst) {
        this.dst = dst;
    }

    public void convertToBase64() {
        this.src = new String(Base64.encodeBase64(src.getBytes()), StandardCharsets.UTF_8);
        this.dst = new String(Base64.encodeBase64(dst.getBytes()), StandardCharsets.UTF_8);
    }
}
