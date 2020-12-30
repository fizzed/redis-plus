
import com.fizzed.blaze.Config;
import com.fizzed.blaze.Contexts;
import static com.fizzed.blaze.Systems.exec;
import static com.fizzed.blaze.Systems.requireExec;
import com.fizzed.blaze.Task;
import org.slf4j.Logger;

public class blaze {
    private final Logger log = Contexts.logger();
    private final Config config = Contexts.config();
    private final String machine = config.value("docker.machine").getOr("default");
    
    @Task(order = 1, value = "Setup dependencies (e.g. redis via docker)")
    public void setup() {
        requireExec("docker", "Please install docker.").run();

        // redis
        exec("docker", "run", "--name", "test-redis",
            "-p", "26379:6379", "-d", "redis:3.2.10")
            .exitValues(0, 125)
            .run();
        
        // fix windows for docker port forwarding
        if (System.getProperty("os.name").contains("Win")) {
            exec("VBoxManage", "controlvm", machine, "natpf1", "tcp26379,tcp,,26379,,26379")
                .exitValues(0, 1).run();
        }
    }
    
    @Task(order = 2, value = "Teardown dependencies (e.g. redis via docker)")
    public void nuke() {
        requireExec("docker").run();
        
        exec("docker", "rm", "-f", "test-redis")
            .exitValues(0, 1)
            .run();
        
        // fix windows for docker port forwarding
        if (System.getProperty("os.name").contains("Win")) {
            exec("VBoxManage", "controlvm", machine, "natpf1", "delete", "tcp26379")
                .exitValues(0, 1).run();
        }
    }
}