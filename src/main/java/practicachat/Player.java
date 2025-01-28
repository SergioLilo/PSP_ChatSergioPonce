package practicachat;


public class Player {

    private String name;
    private int pv;
    private int money;
    private String color;

    public Player(String nombre, String color) {
        this.name = nombre;
        this.pv = ServerStart.PV_INITIAL; // PV inicial
        this.money = ServerStart.MONEY_INITIAL; // Dinero inicial
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public int getPv() {
        return pv;
    }

    public int getMoney() {
        return money;
    }

    public String getColor() {
        return color;
    }

    public void reducirPv(int cantidad) {
        this.pv = Math.max(0, this.pv - cantidad);
    }

    public void reducirDinero(int cantidad) {
        this.money = Math.max(0, this.money - cantidad);
    }

    public void incrementarDinero(int cantidad) {
        this.money += cantidad;
    }

    @Override
    public String toString() {
        return color + name + "\u001B[0m - PV: " + pv + ", Dinero: " + money;
    }
}

