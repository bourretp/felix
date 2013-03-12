package org.apache.felix.ipojo.runtime.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.options.CompositeOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundle;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.*;

/**
 * Bootstrap the test from this project
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class Common {

    @Inject
    protected
    BundleContext bc;

    protected OSGiHelper osgiHelper;
    protected IPOJOHelper ipojoHelper;

    protected boolean deployTestedBundle = true;

    @Configuration
    public Option[] config() throws IOException {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.INFO);

        if (deployTestedBundle) {
            return options(
                    cleanCaches(),
                    ipojoBundles(),
                    junitBundles(),
                    testedBundle(),
                    systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN")
            );
        } else {
            return options(
                    cleanCaches(),
                    ipojoBundles(),
                    junitBundles(),
                    systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN")
            );
        }
    }

    public static Option junitAndMockitoBundles() {
        return new DefaultCompositeOption(
                // Repository required to load harmcrest (OSGi-fied version).
                repository("http://repository.springsource.com/maven/bundles/external").id(
                        "com.springsource.repository.bundles.external"),

                // Mockito without Hamcrest and Objenesis
                mavenBundle("org.mockito", "mockito-core", "1.9.5"),

                // Hamcrest with a version matching the range expected by Mockito
                mavenBundle("org.hamcrest", "com.springsource.org.hamcrest.core", "1.1.0"),

                // Objenesis with a version matching the range expected by Mockito
                wrappedBundle(mavenBundle("org.objenesis", "objenesis", "1.2"))
                        .exports("*;version=1.2"),

                // The default JUnit bundle also exports Hamcrest, but with an (incorrect) version of
                // 4.9 which does not match the Mockito import.
                CoreOptions.junitBundles(),

                /*
                 * Felix has implicit boot delegation enabled by default. It conflicts with Mockito:
                 * java.lang.LinkageError: loader constraint violation in interface itable initialization:
                 * when resolving method "org.osgi.service.useradmin.User$$EnhancerByMockitoWithCGLIB$$dd2f81dc
                 * .newInstance(Lorg/mockito/cglib/proxy/Callback;)Ljava/lang/Object;" the class loader
                 * (instance of org/mockito/internal/creation/jmock/SearchingClassLoader) of the current class,
                 * org/osgi/service/useradmin/User$$EnhancerByMockitoWithCGLIB$$dd2f81dc, and the class loader
                 * (instance of org/apache/felix/framework/BundleWiringImpl$BundleClassLoaderJava5) for interface
                 * org/mockito/cglib/proxy/Factory have different Class objects for the type org/mockito/cglib/
                 * proxy/Callback used in the signature
                 *
                 * So we disable the bootdelegation.
                 */
                frameworkProperty("felix.bootdelegation.implicit").value("false")
        );
    }


    @Before
    public void commonSetUp() {
        osgiHelper = new OSGiHelper(bc);
        ipojoHelper = new IPOJOHelper(bc);

        // Dump OSGi Framework information
        String vendor = (String) osgiHelper.getBundle(0).getHeaders().get(Constants.BUNDLE_VENDOR);
        if (vendor == null) {
            vendor = (String) osgiHelper.getBundle(0).getHeaders().get(Constants.BUNDLE_SYMBOLICNAME);
        }
        String version = (String) osgiHelper.getBundle(0).getHeaders().get(Constants.BUNDLE_VERSION);
        System.out.println("OSGi Framework : " + vendor + " - " + version);
    }

    @After
    public void commonTearDown() {
        ipojoHelper.dispose();
        osgiHelper.dispose();
    }

    public BundleContext getContext() {
        return bc;
    }

    public static ServiceContext getServiceContext(ComponentInstance ci) {
        if (ci instanceof CompositeManager) {
            return ((CompositeManager) ci).getServiceContext();
        } else {
            throw new RuntimeException("Cannot get the service context from a non composite instance");
        }
    }

    public CompositeOption ipojoBundles() {
        return new DefaultCompositeOption(
                mavenBundle("org.apache.felix", "org.apache.felix.ipojo").versionAsInProject(),
                mavenBundle("org.apache.felix", "org.apache.felix.ipojo.composite").versionAsInProject(),
                mavenBundle("org.ow2.chameleon.testing", "osgi-helpers").versionAsInProject(),
                // configuration admin
                mavenBundle("org.apache.felix",  "org.apache.felix.configadmin").versionAsInProject()
        );
    }

    public Option testedBundle() throws MalformedURLException {
        File out = new File("target/tested/bundle.jar");
        if (out.exists()) {
            return bundle(out.toURI().toURL().toExternalForm());
        }

        TinyBundle tested = TinyBundles.bundle();

        // We look inside target/classes to find the class and resources
        File classes = new File("target/classes");
        Collection<File> files = FileUtils.listFilesAndDirs(classes, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        List<File> services = new ArrayList<File>();
        for (File file : files) {
            if (file.isDirectory()) {
                // By convention we export of .services and .service package
                if (file.getName().endsWith("services")  || file.getName().endsWith("service")) {
                    services.add(file);
                }
            } else {
                // We need to compute the path
                String path = file.getAbsolutePath().substring(classes.getAbsolutePath().length() +1);
                tested.add(path, file.toURI().toURL());
                System.out.println(file.getName() + " added to " + path);
            }
        }

        // Export the inherited package, components and strategies
        String export = "";
        for (File file : services) {
            if (export.length() > 0) { export += ", "; }
            String path = file.getAbsolutePath().substring(classes.getAbsolutePath().length() +1);
            String packageName = path.replace('/', '.');
            export += packageName;
        }

        System.out.println("Exported packages : " + export);

        InputStream inputStream = tested
                .set(Constants.BUNDLE_SYMBOLICNAME, "test.bundle")
                .set(Constants.IMPORT_PACKAGE, "*")
                .set(Constants.EXPORT_PACKAGE, export)
                .build(IPOJOStrategy.withiPOJO(new File("src/main/resources")));

        try {
            org.apache.commons.io.FileUtils.copyInputStreamToFile(inputStream, out);
            return bundle(out.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot compute the url of the manipulated bundle");
        } catch (IOException e) {
            throw new RuntimeException("Cannot write of the manipulated bundle");
        }
    }

    public void assertContains(String s, String[] arrays, String object) {
        for (String suspect : arrays) {
            if (object.equals(suspect)) {
                return;
            }
        }
        fail("Assertion failed : " + s);
    }


}
