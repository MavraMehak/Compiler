import java.util.HashMap;
import java.util.Map;

class SymbolTable {
    private final Map<String, SymbolEntry> table;

    public SymbolTable() {
        this.table = new HashMap<>();
    }

    public void insert(String name, String type) {
        if (!table.containsKey(name)) {
            table.put(name, new SymbolEntry(name, type));
        }
    }

    public boolean exists(String name) {
        return table.containsKey(name);
    }

    public SymbolEntry lookup(String name) {
        return table.get(name);
    }
    public void printTable() {
        System.out.println("----------------------------------");
        System.out.println("        Symbol Table");
        System.out.println("----------------------------------");
        System.out.printf("%-15s %-15s\n", "Identifier", "Type");
        System.out.println("----------------------------------");
        for (SymbolEntry entry : table.values()) {
            System.out.printf("%-15s %-15s\n", entry.getName(), entry.getType());
        }
        System.out.println("----------------------------------");
    }
}



