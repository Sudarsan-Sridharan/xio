package com.xjeffrose.xio.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RouteReloaderUnitTest extends Assert {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static final String inputJsonFilename = "input.json";
  private static final String helloWorldValue = "helloworld";
  private static final String badValue = "badValue";
  private static final String changedValue = "changedValue";

  private String rawCreateConf(String value, String filename) throws FileNotFoundException {
    File output = new File(temporaryFolder.getRoot(), filename);
    PrintStream out = new PrintStream(output);
    out.append(value);
    out.flush();
    out.close();
    return output.getAbsolutePath();
  }

  public static class TrivialFactory {
    public String inputString;

    TrivialFactory(String inputString) {
      if (inputString.equals(badValue)) {
        throw new IllegalStateException();
      }
      this.inputString = inputString;
    }
  }

  public static class TrivialState {
    public TrivialFactory oldValue;
    public TrivialFactory newValue;

    public void update(TrivialFactory oldValue, TrivialFactory newValue) {
      this.oldValue = oldValue;
      this.newValue = newValue;
      fireUpdated();
    }

    public void fireUpdated() {}
  }

  private static void addPath(String s) throws Exception {
    File f = new File(s);
    URI u = f.toURI();
    URLClassLoader urlClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class<URLClassLoader> urlClass = URLClassLoader.class;
    Method method = urlClass.getDeclaredMethod("addURL", URL.class);
    method.setAccessible(true);
    method.invoke(urlClassLoader, u.toURL());
  }

  @Before
  public void before() throws Exception {
    addPath(temporaryFolder.getRoot().toString());
  }

  @Test
  public void testInitHappyPath() throws Exception {

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    TrivialFactory initialValue = subject.init(input);
    assertEquals(initialFileContents, initialValue.inputString);
  }

  @Test(expected = IllegalStateException.class)
  public void testInitBadValue() throws Exception {

    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = badValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    TrivialFactory initialValue = subject.init(input);
  }

  @Test
  public void testReload_WhenWatchedFilesDoNotChange() throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    // init the subject with an file that contains the helloworldvalue
    TrivialFactory initialValue = subject.init(input);
    // the result from the init should be the baseline TrivialFactory output and it should match the helloworldvalue
    assertEquals(initialFileContents, initialValue.inputString);

    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    // start the threaded update
    subject.start(state::update);
    Thread.sleep(5000);
    // since we did not change any files we should not get an update
    assertFalse(fireUpdatedCalled.get());
    // the update should have never been called and these values should not be set
    assertEquals(null, state.oldValue);
    assertEquals(null, state.newValue);
    executor.shutdown();
  }

  @Test
  public void testReload_WhenWatchedFilesChange_Date_Was_Modified_and_Digest_Was_NOT_Changed()
      throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    // init the subject with an file that contains the helloworldvalue
    TrivialFactory initialValue = subject.init(input);
    // the result from the init should be the baseline TrivialFactory output and it should match the helloworldvalue
    assertEquals(initialFileContents, initialValue.inputString);

    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    subject.start(state::update);
    Thread.sleep(5000);
    String changedFileContents = helloWorldValue;
    // create new file and overwrite the old file with same value
    String changed = rawCreateConf(changedFileContents, inputJsonFilename);
    Thread.sleep(5000);
    // the update should have never been called since the digest (file contents) did not change
    assertFalse(fireUpdatedCalled.get());
    // these values should not have been set since the update did not occur
    assertEquals(null, state.oldValue);
    assertEquals(null, state.newValue);
    executor.shutdown();
  }

  @Test
  public void testReload_WhenWatchedFilesChange_Date_Was_Modified_and_Digest_Was_Changed()
      throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    // init the subject with an file that contains the helloworldvalue
    TrivialFactory initialValue = subject.init(input);
    // the result from the init should be the baseline TrivialFactory output and it should match the helloworldvalue
    assertEquals(initialFileContents, initialValue.inputString);

    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    subject.start(state::update);
    Thread.sleep(5000);
    String changedFileContents = changedValue;
    // create new file overwrite the old file with different value
    String changed = rawCreateConf(changedFileContents, inputJsonFilename);
    Thread.sleep(5000);
    // the update should have been called since the file contents have changed
    assertTrue(fireUpdatedCalled.get());
    // the old value in the update should be the original value
    assertEquals(initialFileContents, state.oldValue.inputString);
    // the old value in the update should be the changed value
    assertEquals(changedFileContents, state.newValue.inputString);
    executor.shutdown();
  }

  @Test
  public void testReload_WhenWatchedFilesChange_BadUpdate() throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    // init the subject with an file that contains the helloworldvalue
    TrivialFactory initialValue = subject.init(input);
    // the result from the init should be the baseline TrivialFactory output and it should match the helloworldvalue
    assertEquals(initialFileContents, initialValue.inputString);

    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    subject.start(state::update);
    Thread.sleep(5000);
    String changedFileContents = badValue;
    // create new file overwrite the old file with BAD value
    String changed = rawCreateConf(changedFileContents, inputJsonFilename);
    Thread.sleep(5000);
    // the update should have never been called since the update was BAD
    assertFalse(fireUpdatedCalled.get());
    // these values should not have been set since the update did not occur
    assertEquals(null, state.oldValue);
    assertEquals(null, state.newValue);
    executor.shutdown();
  }

  @Test(expected = NullPointerException.class)
  public void testStartWithoutInit() throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    // this should throw an exception since we called start without first calling init
    subject.start(state::update);
  }

  @Test
  public void testStartHappyPath() throws Exception {

    AtomicBoolean fireUpdatedCalled = new AtomicBoolean(false);
    ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

    RouteReloader<TrivialFactory> subject =
        new RouteReloader<TrivialFactory>(executor, TrivialFactory::new);
    String initialFileContents = helloWorldValue;
    String input = rawCreateConf(initialFileContents, inputJsonFilename);
    TrivialFactory initialValue = subject.init(input);
    assertEquals(initialFileContents, initialValue.inputString);

    TrivialState state =
        new TrivialState() {
          @Override
          public void fireUpdated() {
            fireUpdatedCalled.set(true);
          }
        };

    // this should NOT throw an exception because we called init before calling start
    subject.start(state::update);
    executor.shutdown();
  }
}
