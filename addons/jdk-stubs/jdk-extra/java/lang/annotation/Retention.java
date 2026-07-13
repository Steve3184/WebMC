package java.lang.annotation;

public @interface Retention {
    RetentionPolicy value() default RetentionPolicy.CLASS;
}
