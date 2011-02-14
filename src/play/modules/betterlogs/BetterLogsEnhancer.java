package play.modules.betterlogs;

import javassist.CannotCompileException;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import play.classloading.ApplicationClasses.ApplicationClass;
import play.classloading.enhancers.Enhancer;

public class BetterLogsEnhancer extends Enhancer {
	@Override
	public void enhanceThisClass(final ApplicationClass applicationClass) throws Exception {
		final CtClass ctClass = makeClass(applicationClass);
		for(final CtBehavior behavior : ctClass.getDeclaredBehaviors()) {
			behavior.instrument(new ExprEditor() {
				@Override
				public void edit(MethodCall m) throws CannotCompileException {
					try {
						if("play.Logger".equals(m.getClassName())) {
							String name = m.getMethodName();
							System.out.println(" method is " + behavior.toString());
							//String level, String clazz, String clazzSimpleName, String packageName, String method, String signature, String fileName, String relativeFileName, int line, Object[] args
							if("trace".equals(name) || "debug".equals(name) || "info".equals(name) || "warn".equals(name) || "error".equals(name) || "fatal".equals(name)) {
								String code = String.format("{play.modules.betterlogs.BetterLogsPlugin.log(\"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", \"%s\", %s, %s);}",
										name,
										ctClass.getName(), // canonical name
										ctClass.getSimpleName(), // simple name
										ctClass.getPackageName(), // package
										behavior.getName(),
										behavior.getSignature(),
										m.getFileName(),
										applicationClass.javaFile.relativePath(),
										m.getLineNumber(),
										"$args" // original args
								);
								System.out.println("would ' " + code + "'");
								m.replace(code);
							}
						}
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		applicationClass.enhancedByteCode = ctClass.toBytecode();
		ctClass.defrost();
	}
}
