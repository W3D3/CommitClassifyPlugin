package data;

public class Matcher {
    public int id;
    public String name;

    public Matcher() {
        this.id = -1;
        this.name = "";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Matcher)) return false;
        return this.getId() == ((Matcher) obj).getId() && this.getName().equals(((Matcher) obj).getName());
    }
}
