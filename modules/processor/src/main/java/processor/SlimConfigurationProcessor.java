package processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

@SupportedAnnotationTypes({ "*" })
public class SlimConfigurationProcessor extends AbstractProcessor {

	private final static String SLIM_STATE_PATH = "META-INF/"
			+ "slim-configuration-processor.properties";

	private Filer filer;

	private Messager messager;

	private InitializerSpecs specs;

	private ElementUtils utils;

	private boolean processed;

	private Imports imports;

	private Components components;

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.filer = processingEnv.getFiler();
		this.messager = processingEnv.getMessager();
		this.utils = new ElementUtils(processingEnv.getTypeUtils(),
				processingEnv.getElementUtils(), this.messager);
		this.imports = new Imports(this.utils);
		this.components = new Components(this.utils);
		loadState();
		this.specs = new InitializerSpecs(this.utils, this.imports, this.components);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations,
			RoundEnvironment roundEnv) {
		// messager.printMessage(Kind.NOTE, "processor instance running
		// #"+Integer.toHexString(System.identityHashCode(this)));
		if (roundEnv.processingOver()) {
			saveState();
		}
		else if (!processed) {
			process(roundEnv);
			processed = true;
		}
		return true;
	}

	private Set<TypeElement> collectTypes(RoundEnvironment roundEnv,
			Predicate<TypeElement> typeSelectionCondition) {
		Set<TypeElement> types = new HashSet<>();
		for (TypeElement type : ElementFilter.typesIn(roundEnv.getRootElements())) {
			collectTypes(type, types, typeSelectionCondition);
		}
		return types;
	}

	private void collectTypes(TypeElement type, Set<TypeElement> types,
			Predicate<TypeElement> typeSelectionCondition) {
		if (typeSelectionCondition.test(type)) {
			types.add(type);
			for (Element element : type.getEnclosedElements()) {
				if (element instanceof TypeElement) {
					if (utils.hasAnnotation(element,
							SpringClassNames.CONFIGURATION.toString())) {
						messager.printMessage(Kind.NOTE,
								"Found nested @Configuration in " + element, element);
						imports.addNested(type, (TypeElement) element);
					}
					collectTypes((TypeElement) element, types, typeSelectionCondition);
				}
			}
		}
	}

	private void process(RoundEnvironment roundEnv) {
		Set<TypeElement> types = collectTypes(roundEnv,
				te -> te.getKind() == ElementKind.CLASS
						&& !te.getModifiers().contains(Modifier.ABSTRACT)
						&& !te.getModifiers().contains(Modifier.STATIC));
		for (TypeElement type : types) {
			if (utils.hasAnnotation(type, SpringClassNames.CONFIGURATION.toString())) {
				messager.printMessage(Kind.NOTE, "Found @Configuration in " + type, type);
				specs.addInitializer(type);
			}
			components.addComponent(type);
		}
		// Hoover up any imports that didn't already get turned into initializers
		for (TypeElement importer : imports.getImports().keySet()) {
			for (TypeElement imported : imports.getImports(importer)) {
				String root = utils.getPackage(importer);
				// Only if they are in the same package (a reasonable proxy for "in
				// this source module")
				if (utils.getPackage(imported).equals(root) && !utils.isImporter(imported)) {
					specs.addInitializer(imported);
				}
			}
		}
		// Work out what these modules include
		for (InitializerSpec initializer : specs.getInitializers()) {
			messager.printMessage(Kind.NOTE,
					"Writing Initializer " + ClassName.get(initializer.getPackage(),
							initializer.getInitializer().name),
					initializer.getConfigurationType());
			write(initializer.getInitializer(), initializer.getPackage());
		}
	}

	private void write(TypeSpec type, String packageName) {
		JavaFile file = JavaFile.builder(packageName, type).build();
		try {
			file.writeTo(this.filer);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	public void loadState() {
		Properties properties = new Properties();
		try {
			FileObject resource = filer.getResource(StandardLocation.CLASS_OUTPUT, "",
					SLIM_STATE_PATH);
			try (InputStream stream = resource.openInputStream();) {
				properties.load(stream);
			}
			messager.printMessage(Kind.NOTE,
					"Loading imports information from previous build:" + properties);
			for (Map.Entry<Object, Object> property : properties.entrySet()) {
				String annotationType = (String) property.getKey(); // registrarinitializer.XXXX.YYY.ZZZ
				String k = annotationType.substring("import.".length());
				TypeElement kte = utils.asTypeElement(k);
				for (String v : ((String) property.getValue()).split(",")) {
					TypeElement vte = utils.asTypeElement(v);
					if (kte == null || vte == null) {
						// TODO need to cope with types being removed across incremental
						// builds - is this ok?
						messager.printMessage(Kind.NOTE,
								"Looks like a type has been removed, ignoring registrar entry "
										+ k + "=" + v + " resolved to " + kte + "="
										+ vte);
					}
					else {
						imports.addImport(kte, vte);
					}
				}
			}
			messager.printMessage(Kind.NOTE,
					"Loaded " + properties.size() + " import definitions");
		}
		catch (IOException e) {
			messager.printMessage(Kind.NOTE,
					"Cannot load " + SLIM_STATE_PATH + " (normal on first full build)");
		}
	}

	// TODO merge moduleSpecs state into just one overall annotation processor state,
	// rather than multiple files
	public void saveState() {
		Properties properties = new Properties();
		for (Map.Entry<TypeElement, Set<TypeElement>> entry : imports.getImports()
				.entrySet()) {
			properties.setProperty(
					"import." + entry.getKey().getQualifiedName().toString(),
					entry.getValue().stream()
							.map(value -> value.getQualifiedName().toString())
							.collect(Collectors.joining(",")));
		}
		try {
			FileObject resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "",
					SLIM_STATE_PATH);
			try (OutputStream stream = resource.openOutputStream();) {
				properties.store(stream, "Created by " + getClass().getName());
			}
		}
		catch (IOException e) {
			messager.printMessage(Kind.NOTE, "Cannot write " + SLIM_STATE_PATH);
		}
	}

}
