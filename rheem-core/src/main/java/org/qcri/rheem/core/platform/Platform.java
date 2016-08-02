package org.qcri.rheem.core.platform;

import org.qcri.rheem.core.api.Configuration;
import org.qcri.rheem.core.api.Job;
import org.qcri.rheem.core.api.exception.RheemException;
import org.qcri.rheem.core.optimizer.costs.LoadProfileToTimeConverter;
import org.qcri.rheem.core.plan.executionplan.Channel;
import org.qcri.rheem.core.plan.executionplan.ExecutionTask;
import org.qcri.rheem.core.plan.executionplan.PlatformExecution;
import org.qcri.rheem.core.plan.rheemplan.ExecutionOperator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A platform describes an execution engine that executes {@link ExecutionOperator}s.
 */
public abstract class Platform {

    private final String name;

    /**
     * Loads a specific {@link Platform} implementation. For platforms to interoperate with this method, they must
     * provide a {@code static}, parameterless method {@code getInstance()} that returns their singleton instance.
     *
     * @param platformClassName the class name of the {@link Platform}
     * @return the {@link Platform} instance
     */
    public static Platform load(String platformClassName) {
        final Class<?> platformClass;
        try {
            platformClass = Class.forName(platformClassName);
            final Method getInstanceMethod = platformClass.getMethod("getInstance");
            return (Platform) getInstanceMethod.invoke(null);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RheemException("Could not load platform: " + platformClassName, e);
        }
    }

    protected Platform(String name) {
        this.name = name;
        this.configureDefaults(Configuration.getDefaultConfiguration());
    }

    /**
     * Configure default settings for this instance, e.g., to be able to create {@link LoadProfileToTimeConverter}s.
     *
     * @param configuration that should be configured
     */
    protected abstract void configureDefaults(Configuration configuration);

    /**
     * <i>Shortcut.</i> Creates an {@link Executor} using the {@link #getExecutorFactory()}.
     *
     * @return the {@link Executor}
     */
    public Executor createExecutor(Job job) {
        return this.getExecutorFactory().create(job);
    }

    public abstract Executor.Factory getExecutorFactory();

    public String getName() {
        return this.name;
    }

    // TODO: Return some more descriptors about the state of the platform (e.g., available machines, RAM, ...)?

    @Override
    public String toString() {
        return String.format("Platform[%s]", this.getName());
    }

    /**
     * Tells whether the given constellation of producing and consuming {@link ExecutionTask}, linked by the
     * {@link Channel} can be handled within a single {@link PlatformExecution} of this {@link Platform}
     *
     * @param producerTask an {@link ExecutionTask} running on this {@link Platform}
     * @param channel      links the {@code producerTask} and {@code consumerTask}
     * @param consumerTask an  {@link ExecutionTask} running on this {@link Platform}
     * @return whether the {@link ExecutionTask}s can be executed in a single {@link PlatformExecution}
     */
    public boolean isSinglePlatformExecutionPossible(ExecutionTask producerTask, Channel channel, ExecutionTask consumerTask) {
        assert producerTask.getOperator().getPlatform() == this;
        assert consumerTask.getOperator().getPlatform() == this;
        assert channel.getProducer() == producerTask;
        assert channel.getConsumers().contains(consumerTask);

        // Overwrite as necessary.
        return true;
    }

    /**
     * @return a default {@link LoadProfileToTimeConverter}
     */
    public abstract LoadProfileToTimeConverter createLoadProfileToTimeConverter(Configuration configuration);

    /**
     * Warm up this instance.
     *
     * @param configuration used to warm up
     */
    public void warmUp(Configuration configuration) {
        // Do nothing by default.
    }

    /**
     * Get the time necessary to initialize this instance and use it for execution.
     *
     * @return the milliseconds required to initialize this instance
     */
    public long getInitializeMillis(Configuration configuration) {
        return 0L;
    }

}
