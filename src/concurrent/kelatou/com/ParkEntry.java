package concurrent.kelatou.com;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ParkEntry {

    // the number limit of visitors
    public static int MAX_ENTRY = 50;

    final static AtomicInteger seq = new AtomicInteger();

    public static void main(String[] args) throws Exception {
        if (args.length > 0)
            MAX_ENTRY = new Integer(args[0]);

        CountDownLatch cldComer = new CountDownLatch(1);
        CountDownLatch cldEnter = new CountDownLatch(1);

        ExecutorService exec = Executors.newCachedThreadPool();

        // 4 park doors
        Door[] doors = { new Door("EAST", seq, exec, cldEnter), new Door("WEST", seq, exec, cldEnter),
                new Door("SOUTH", seq, exec, cldEnter), new Door("NORTH", seq, exec, cldEnter) };

        Random randDoor = new Random(100);

        // Simulate coming vistors who want to enter park
        exec.execute(new Runnable() {
            @Override
            public void run() {
                VistorGenerator vistorGenerator = new VistorGenerator();
                while (!Thread.interrupted()) {
                    if (seq.get() < MAX_ENTRY) {
                        doors[randDoor.nextInt(doors.length)].vistorComes(vistorGenerator.next());
                    } else {
                        cldComer.countDown();
                    }
                }
            }
        });

        // Simulate vistor is allowed to enter the park
        exec.execute(new Runnable() {

            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    doors[randDoor.nextInt(doors.length)].checkAndAllowToEnter();
                }
            }
        });

        exec.execute(new Runnable() {
            @Override
            public void run() {

                try {
                    cldEnter.await();
                    cldComer.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (Door door : doors) {
                    door.showWaitingVisitorsInfo();
                }

                exec.shutdownNow();
            }
        });
    }
}

class Door {
    private String name;
    private final ConcurrentLinkedQueue<Visitor> waitingQueue = new ConcurrentLinkedQueue<Visitor>();
    private AtomicInteger seq;
    final ExecutorService exec;
    final CountDownLatch cld;

    public Door(final String name, final AtomicInteger seq, final ExecutorService exec, final CountDownLatch cld) {
        this.name = name;
        this.seq = seq;
        this.exec = exec;
        this.cld = cld;
    }

    public void vistorComes(Visitor visitor) {
        waitingQueue.offer(visitor);
        System.out.println(String.format("Vistor %s comes to door %s in order to enter park", visitor.getName(), name));
    }

    public void checkAndAllowToEnter() {
        if (seq.get() >= ParkEntry.MAX_ENTRY) {
            cld.countDown();
            return;
        }

        if (waitingQueue.peek() != null) {
            if (seq.incrementAndGet() <= ParkEntry.MAX_ENTRY) {
                System.out.println(String.format("Vistor %s is allowed to enter pard from door %s, with SEQ%d",
                        waitingQueue.poll().getName(), name, seq.get()));
            }
        }
    }

    public void showWaitingVisitorsInfo() {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%d visitors are still waiting at door %s:", waitingQueue.size(), name));

        // Since 1.8
        String vistors = waitingQueue.stream()
                .map(i -> i.getName())
                .collect(Collectors.joining(", "));
        sb.append(vistors);

        System.out.println(sb.toString());
    }
}

class VistorGenerator implements Generator<Visitor> {

    private static Sex[] sexArray = { Sex.FEMALE, Sex.MALE };

    Random rand = new Random(100);

    @Override
    public Visitor next() {
        try {
            Visitor vistor = Visitor.class.newInstance();
            vistor.setName(UUID.randomUUID().toString());
            vistor.setSex(sexArray[rand.nextInt(sexArray.length)]);
            vistor.setAge(rand.nextInt(100));
            return vistor;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

enum Sex {
    MALE, FEMALE
}

class Visitor {

    private String name;
    private Sex sex;
    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Sex getSex() {
        return sex;
    }

    public void setSex(Sex sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
