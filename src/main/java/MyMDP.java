import org.deeplearning4j.gym.StepReply;
import org.deeplearning4j.rl4j.mdp.MDP;
import org.deeplearning4j.rl4j.space.ActionSpace;
import org.deeplearning4j.rl4j.space.ObservationSpace;

public class MyMDP<O, A, AS extends ActionSpace<A>> implements MDP<O, A, AS> {

    @Override
    public ObservationSpace<O> getObservationSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public AS getActionSpace() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public O reset() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public StepReply<O> step(A action) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public MDP<O, A, AS> newInstance() {
        // TODO Auto-generated method stub
        return null;
    }

}