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

		CountDownLatch cldComer = new CountDownLatch(4);

		ExecutorService exec = Executors.newCachedThreadPool();

		// 4 park doors
		Door[] doors = { new Door("EAST"), new Door("WEST"), new Door("SOUTH"), new Door("NORTH") };

		// Simulate coming visitors who want to enter park
		for (Door door : doors) {
			exec.execute(new VisitorsCome(door, seq, cldComer, MAX_ENTRY));
		}

		// Simulate visitor is allowed to enter the park
		for (Door door : doors) {
			exec.execute(new visitorsEnter(door, seq, MAX_ENTRY));
		}

		// print name list who are still waiting at the doors
		exec.execute(new Runnable() {
			@Override
			public void run() {

				try {
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

class VisitorsCome implements Runnable {
	final Door door;
	final CountDownLatch cldComer;
	final AtomicInteger seq;
	final int maxLimit;

	VisitorsCome(final Door door, final AtomicInteger seq, final CountDownLatch cldComer, final int maxLimit) {
		this.door = door;
		this.cldComer = cldComer;
		this.seq = seq;
		this.maxLimit = maxLimit;
	}

	@Override
	public void run() {
		VistorGenerator vistorGenerator = new VistorGenerator();
		while (seq.get() < maxLimit) {
			door.vistorComes(vistorGenerator.next());

		}
		cldComer.countDown();
	}
}

class visitorsEnter implements Runnable {
	final Door door;
	final AtomicInteger seq;
	final int maxLimit;

	visitorsEnter(final Door door, final AtomicInteger seq, final int maxLimit) {
		this.door = door;
		this.seq = seq;
		this.maxLimit = maxLimit;
	}

	@Override
	public void run() {
		while (seq.get() < maxLimit) {
			if (door.isVistsitorInQueue()) {
				int newSequence = seq.incrementAndGet();
				if (newSequence <= maxLimit) {
					door.visitorEnters(newSequence);
				}
			}
		}
	}
}

class Door {
	private String name;
	private final ConcurrentLinkedQueue<Visitor> waitingQueue = new ConcurrentLinkedQueue<Visitor>();

	public Door(final String name) {
		this.name = name;
	}

	public void vistorComes(Visitor visitor) {
		waitingQueue.offer(visitor);
		System.out.println(String.format("Vistor %s comes to door %s in order to enter park", visitor.getName(), name));
	}

	public void visitorEnters(int sequence) {
		if (waitingQueue.peek() != null) {
			System.out.println(String.format("Vistor %s is allowed to enter pard from door %s, with SEQ%d",
					waitingQueue.poll().getName(), name, sequence));
		}
	}

	public void showWaitingVisitorsInfo() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("%d visitors are still waiting at door %s:", waitingQueue.size(), name));

		// Since 1.8
		String vistors = waitingQueue.stream().map(i -> i.getName()).collect(Collectors.joining(", "));
		sb.append(vistors);

		System.out.println(sb.toString());
	}

	public boolean isVistsitorInQueue() {
		return waitingQueue.peek() != null;
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
