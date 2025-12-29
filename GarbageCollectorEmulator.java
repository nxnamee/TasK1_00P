import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Класс для представления объекта в памяти
class MemoryObject {
    private final int id;
    private final int size;
    private final List<Integer> references;
    private boolean marked;
    
    public MemoryObject(int id, int size) {
        this.id = id;
        this.size = size;
        this.references = new ArrayList<>();
        this.marked = false;
    }
    
    public int getId() { return id; }
    public int getSize() { return size; }
    public List<Integer> getReferences() { return references; }
    public boolean isMarked() { return marked; }
    public void setMarked(boolean marked) { this.marked = marked; }
    public void addReference(int objectId) { references.add(objectId); }
}

// Менеджер памяти
class MemoryManager {
    private final Map<Integer, MemoryObject> heap;
    private final Set<Integer> roots;
    private final int maxMemory;
    private int usedMemory;
    private int objectCounter;
    
    public MemoryManager(int maxMemory) {
        this.heap = new HashMap<>();
        this.roots = new HashSet<>();
        this.maxMemory = maxMemory;
        this.usedMemory = 0;
        this.objectCounter = 0;
    }
    
    public MemoryObject allocate(int size, boolean isRoot) {
        if (usedMemory + size > maxMemory) {
            System.out.println("Недостаточно памяти! Запуск сборщика мусора...\n");
            garbageCollect();
            
            if (usedMemory + size > maxMemory) {
                System.out.println("Ошибка: недостаточно памяти даже после GC!");
                return null;
            }
        }
        
        MemoryObject obj = new MemoryObject(objectCounter++, size);
        heap.put(obj.getId(), obj);
        usedMemory += size;
        
        if (isRoot) {
            roots.add(obj.getId());
        }
        
        System.out.println("✅ Создан объект ID=" + obj.getId() + " размером " + size + " байт" + (isRoot ? " (корень)" : ""));
        return obj;
    }
    
    public void addReference(int fromId, int toId) {
        MemoryObject from = heap.get(fromId);
        if (from != null && heap.containsKey(toId)) {
            from.addReference(toId);
            System.out.println("🔗 Добавлена ссылка: объект " + fromId + " → объект " + toId);
        }
    }
    
    public void removeRoot(int objectId) {
        roots.remove(objectId);
        System.out.println("🗑️  Удален корень: объект " + objectId);
    }
    
    // Mark-and-Sweep алгоритм
    public void garbageCollect() {
        System.out.println("\n╔════════════════════════════════════════╗");
        System.out.println("║   🔄 ЗАПУСК СБОРКИ МУСОРА (Mark&Sweep) ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Фаза Mark
        System.out.println(" Фаза MARK: маркировка достижимых объектов");
        mark();
        
        // Фаза Sweep
        System.out.println("\n Фаза SWEEP: удаление недостижимых объектов");
        sweep();
        
        System.out.println("\n Сборка мусора завершена!\n");
    }
    
    private void mark() {
        // Сброс всех меток
        for (MemoryObject obj : heap.values()) {
            obj.setMarked(false);
        }
        
        // Обход в глубину от корневых объектов
        for (int rootId : roots) {
            markRecursive(rootId);
        }
    }
    
    private void markRecursive(int objectId) {
        MemoryObject obj = heap.get(objectId);
        if (obj == null || obj.isMarked()) {
            return;
        }
        
        obj.setMarked(true);
        System.out.println("   ✓ Помечен объект ID=" + objectId);
        
        for (int refId : obj.getReferences()) {
            markRecursive(refId);
        }
    }
    
    private void sweep() {
        List<Integer> toRemove = new ArrayList<>();
        
        for (Map.Entry<Integer, MemoryObject> entry : heap.entrySet()) {
            if (!entry.getValue().isMarked()) {
                toRemove.add(entry.getKey());
            }
        }
        
        if (toRemove.isEmpty()) {
            System.out.println("   ℹ️  Мусора не найдено");
        } else {
            for (int id : toRemove) {
                MemoryObject obj = heap.remove(id);
                usedMemory -= obj.getSize();
                System.out.println("   🗑️  Удален объект ID=" + id + " (" + obj.getSize() + " байт)");
            }
        }
    }
    
    public void printStatus() {
        System.out.println("\n┌─────────────────────────────────┐");
        System.out.println("│     📊 СОСТОЯНИЕ ПАМЯТИ         │");
        System.out.println("└─────────────────────────────────┘");
        System.out.println("Использовано: " + usedMemory + " / " + maxMemory + " байт");
        System.out.println("Объектов в куче: " + heap.size());
        System.out.println("Корневых объектов: " + roots.size());
        
        double usage = (double) usedMemory / maxMemory * 100;
        int bars = (int) (usage / 5);
        System.out.print("Заполнение: [");
        for (int i = 0; i < 20; i++) {
            System.out.print(i < bars ? "█" : "░");
        }
        System.out.printf("] %.1f%%\n", usage);
        
        if (!heap.isEmpty()) {
            System.out.println("\nОбъекты в памяти:");
            for (MemoryObject obj : heap.values()) {
                String rootMark = roots.contains(obj.getId()) ? " 🌳" : "";
                System.out.println("  • ID=" + obj.getId() + " | размер=" + obj.getSize() + 
                                 " | ссылки=" + obj.getReferences() + rootMark);
            }
        }
        System.out.println();
    }
}

// Главный класс приложения
public class GarbageCollectorEmulator {
    
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║  🗑️  ЭМУЛЯТОР СБОРЩИКА МУСОРА (Mark&Sweep)  ║");
        System.out.println("╚══════════════════════════════════════════════╝\n");
        
        MemoryManager memory = new MemoryManager(1000);
        Scanner scanner = new Scanner(System.in);
        
        // Демонстрационный сценарий
        runDemo(memory);
        
        // Интерактивный режим
        interactiveMode(memory, scanner);
    }
    
    private static void runDemo(MemoryManager memory) {
        System.out.println("🎬 Запуск демонстрационного сценария...\n");
        pause(1000);
        
        // Создание объектов
        MemoryObject obj1 = memory.allocate(100, true);  // Корневой
        pause(500);
        MemoryObject obj2 = memory.allocate(150, true);  // Корневой
        pause(500);
        MemoryObject obj3 = memory.allocate(200, false); // Обычный
        pause(500);
        MemoryObject obj4 = memory.allocate(80, false);  // Обычный
        pause(500);
        MemoryObject obj5 = memory.allocate(120, false); // Обычный
        
        System.out.println();
        pause(800);
        
        // Создание ссылок
        memory.addReference(obj1.getId(), obj3.getId());
        pause(500);
        memory.addReference(obj2.getId(), obj4.getId());
        pause(500);
        memory.addReference(obj3.getId(), obj5.getId());
        
        memory.printStatus();
        pause(2000);
        
        // Удаление корня - obj2 станет недостижим вместе с obj4
        memory.removeRoot(obj2.getId());
        memory.printStatus();
        pause(2000);
        
        // Попытка выделить еще память
        memory.allocate(400, true);
        memory.printStatus();
        pause(2000);
        
        // Удаление еще одного корня
        memory.removeRoot(obj1.getId());
        memory.printStatus();
        pause(2000);
        
        // GC очистит obj1, obj3, obj5
        memory.garbageCollect();
        memory.printStatus();
        
        System.out.println("✅ Демонстрация завершена!\n");
    }
    
    private static void interactiveMode(MemoryManager memory, Scanner scanner) {
        System.out.println("╔═══════════════════════════════════╗");
        System.out.println("║   🎮 ИНТЕРАКТИВНЫЙ РЕЖИМ          ║");
        System.out.println("╚═══════════════════════════════════╝\n");
        
        while (true) {
            System.out.println("Команды:");
            System.out.println("  1 - Создать объект");
            System.out.println("  2 - Добавить ссылку");
            System.out.println("  3 - Удалить корень");
            System.out.println("  4 - Запустить GC");
            System.out.println("  5 - Показать состояние");
            System.out.println("  0 - Выход");
            System.out.print("\nВыберите команду: ");
            
            int choice = scanner.nextInt();
            System.out.println();
            
            switch (choice) {
                case 1:
                    System.out.print("Размер объекта (байт): ");
                    int size = scanner.nextInt();
                    System.out.print("Сделать корнем? (1-да, 0-нет): ");
                    boolean isRoot = scanner.nextInt() == 1;
                    memory.allocate(size, isRoot);
                    System.out.println();
                    break;
                    
                case 2:
                    System.out.print("ID объекта-источника: ");
                    int fromId = scanner.nextInt();
                    System.out.print("ID объекта-цели: ");
                    int toId = scanner.nextInt();
                    memory.addReference(fromId, toId);
                    System.out.println();
                    break;
                    
                case 3:
                    System.out.print("ID корневого объекта: ");
                    int rootId = scanner.nextInt();
                    memory.removeRoot(rootId);
                    System.out.println();
                    break;
                    
                case 4:
                    memory.garbageCollect();
                    break;
                    
                case 5:
                    memory.printStatus();
                    break;
                    
                case 0:
                    System.out.println("👋 Завершение работы...");
                    scanner.close();
                    return;
                    
                default:
                    System.out.println("❌ Неизвестная команда\n");
            }
        }
    }
    
    private static void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}