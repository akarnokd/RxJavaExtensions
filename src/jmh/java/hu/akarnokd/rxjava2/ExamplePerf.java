package hu.akarnokd.rxjava2;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

/**
 * Example benchmark. Run from command line as
 * <br>
 * gradle jmh -Pjmh='ExamplePerf'
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 5)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1)
@State(Scope.Thread)
public class ExamplePerf {

    @Param({"1", "10", "100", "1000", "10000", "100000", "1000000"})
    public int count;
    
    @Setup
    public void setup() {

    }

}