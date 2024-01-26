package game.SY.ai;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManagedAI {

	enum VisualiserType {

		MAP_OVERLAP,


		WINDOWED,


		NONE
	}

	String value();


	VisualiserType visualiserType() default VisualiserType.NONE;

}
