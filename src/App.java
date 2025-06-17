import impl.TaskExecutorService;
import service.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class App {

    public static void main(String[] Args) {
        TaskExecutorService executorService= new TaskExecutorService(3);
        Main.TaskGroup group1= new Main.TaskGroup(UUID.randomUUID());
        Main.TaskGroup group2= new Main.TaskGroup(UUID.randomUUID());
        List<Future<String>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                final int id = i;
                Future<String> future=executorService.submitTask(new Main.Task<>(
                        UUID.randomUUID(), group1, Main.TaskType.READ,
                        () -> {
                            System.out.println("Group1 read "+id+" started");
                            Thread.sleep(2000);
                            System.out.println("Group1 read "+id+ " done");
                            return "Group1 READ"+id;
                        }
                ));
                futures.add(future);
            }

            Future<String> group1Write= executorService.submitTask(new Main.Task<>(
                    UUID.randomUUID(), group1, Main.TaskType.WRITE,
                    () -> {
                        System.out.println("Group1 write started");
                        Thread.sleep(3000);
                        System.out.println("Group1 write done");
                        return "Group1 WRITE";
                    }
            ));
            futures.add(group1Write);

            Future<String> group2Write=executorService.submitTask(new Main.Task<>(
                    UUID.randomUUID(), group2, Main.TaskType.WRITE,
                    () -> {
                        System.out.println("Group2 write started");
                        Thread.sleep(2500);
                        System.out.println("Group2 write done");
                        return "Group2 WRITE";
                    }
            ));
            futures.add(group2Write);

            Future<String> group2Read=executorService.submitTask(new Main.Task<>(
                    UUID.randomUUID(), group2, Main.TaskType.READ,
                    () -> {
                        System.out.println("Group2 read started");
                        Thread.sleep(1000);
                        System.out.println("Group2 read done");
                        return "Group2 READ";
                    }
            ));
            futures.add(group2Read);

            for (Future<String> future : futures) {
                try {
                    String result = future.get();  // Blocks until result is ready
                    System.out.println("Result: "+ result);
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Task failed: "+ e.getMessage());
                }
            }
        }
        finally {
            executorService.shutdown();
        }
    }
}
