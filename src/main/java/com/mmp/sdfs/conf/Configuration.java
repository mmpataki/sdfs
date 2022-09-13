package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class Configuration {

    @Argument(keys = {"-h", "--help"}, help = "Prints help")
    boolean help;

    @Argument(keys = {"--props"}, help = "Config props file")
    String propsFile;

    @Argument(keys = {"--printProps"}, help = "Prints sample props file")
    boolean printProps = false;


    BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));

    // internal for debugging
    boolean __debug = false;

    @Override
    public String toString() {
        return getFields().stream().map(f -> {
            try {
                return String.format("%s = %s", f.getName(), f.get(this));
            } catch (IllegalAccessException e) {
                return "";
            }
        }).collect(Collectors.joining("\n"));
    }

    public void defaultParser(Field f, Configuration c, String s) throws Exception {
        Object val = null;
        if (Integer.TYPE.isAssignableFrom(f.getType())) {
            val = Integer.parseInt(s);
        } else if (Long.TYPE.isAssignableFrom(f.getType())) {
            val = Long.parseLong(s);
        } else if (Double.TYPE.isAssignableFrom(f.getType())) {
            val = Double.parseDouble(s);
        } else if (Float.TYPE.isAssignableFrom(f.getType())) {
            val = Float.parseFloat(s);
        } else if (Boolean.TYPE.isAssignableFrom(f.getType())) {
            val = s == null || Boolean.parseBoolean(s);
        } else {
            val = s.isEmpty() ? null : s;
        }
        boolean accessible = f.isAccessible();
        f.setAccessible(true);
        f.set(c, val);
        f.setAccessible(accessible);
    }

    public void classInstatiator(Field f, Configuration c, String s) throws Exception {
        f.set(c, Class.forName(s).newInstance());
    }

    private void set(Field f, String val) throws Exception {
        getClass().getMethod(f.getAnnotation(Argument.class).parser(), Field.class, Configuration.class, String.class)
                .invoke(this, f, this, val);
    }

    public void getFields(Class<?> cls, List<Field> fields) {
        Arrays.stream(cls.getDeclaredFields()).filter(f -> f.isAnnotationPresent(Argument.class)).forEach(fields::add);
        if (cls != Configuration.class) {
            getFields(cls.getSuperclass(), fields);
        }
    }

    public List<Field> getFields() {
        List<Field> l = new ArrayList<>();
        getFields(this.getClass(), l);
        return l;
    }


    public Configuration(String args[]) throws Exception {
        Map<String, Field> fields = new LinkedHashMap<>();
        List<Field> allFields = getFields();
        allFields.forEach(f -> {
            Argument arg = f.getAnnotation(Argument.class);
            for (String key : arg.keys())
                fields.put(key, f);
        });

        // set default values
        for (Field field : allFields) {
            Argument arg = field.getAnnotation(Argument.class);
            if (!arg.defValue().isEmpty()) {
                set(field, field.getType().isAssignableFrom(boolean.class) ? "true" : arg.defValue());
            }
        }

        // override values
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!fields.containsKey(arg)) {
                log.warn("unknown argument: " + arg);
                System.exit(0);
            }
            Field field = fields.get(arg);
            set(field, field.getType().isAssignableFrom(boolean.class) ? "true" : args[++i]);
        }

        if (propsFile != null) {
            Properties props = new Properties();
            props.load(new FileReader(propsFile));
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                set(getClass().getDeclaredField(entry.getKey().toString()), entry.getValue().toString());
            }
        }

        if (printProps) {
            for (Field f : allFields) {
                if (f.isAnnotationPresent(Argument.class)) {
                    Argument arg = f.getAnnotation(Argument.class);
                    System.out.printf("# %s\n", arg.required() ? "REQUIRED" : "OPTIONAL");
                    System.out.printf("# %s\n", f.getAnnotation(Argument.class).help());
                    boolean noVal = f.get(this) == null || Map.class.isAssignableFrom(f.getType()) || Collection.class.isAssignableFrom(f.getType());
                    System.out.printf("%s=%s\n", f.getName(), noVal ? "" : f.get(this));
                    System.out.println();
                }
            }
            System.exit(0);
        }
        if (!help) {
            for (Field f : allFields) {
                Argument argConf = f.getAnnotation(Argument.class);
                if (!(f.getAnnotation(Argument.class).required() && f.get(this) == null)) continue;
                System.out.printf("Enter %s (%s): ", f.getName(), argConf.help());
                set(f, (argConf.sensitive()) ? new String(System.console().readPassword()) : sc.readLine());
            }
        }
    }

    public String getHelpString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("   %-35s %4s %8s %s\n", "switch", "reqd", "multiple", "help"));

        getFields().forEach(f -> {
            Argument arg = f.getAnnotation(Argument.class);
            sb.append(String.format(
                    "   %-35s %3s   %4s    %s\n",
                    String.join(", ", arg.keys()) + (f.getType().isAssignableFrom(boolean.class) ? "" : String.format(" <%s>", f.getName())),
                    arg.required() ? "*" : " ",
                    arg.multivalued() ? "*" : " ",
                    arg.help() + " (default: " + arg.defValue() + ")"
            ));
        });
        return sb.toString();
    }
}