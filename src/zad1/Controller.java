package zad1;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import zad1.models.Bind;

public class Controller {
	private Class<?> modelClass = null;
	private Object modelObject = null;
	private ScriptEngineManager manager;
	private ScriptEngine engine;
	public Controller(String modelName) {
		try {
			  String modelFullName = "zad1.models." + modelName; 
		      modelClass = Class.forName(modelFullName);
		      modelObject = modelClass.newInstance();
		    } catch (Exception exc) {
		      throw new RuntimeException("Wrong model name ");
		    }
		manager = new ScriptEngineManager();
		engine = manager.getEngineByName("groovy");
		engine.createBindings();
	}

	public Controller readDataFrom(String fname) {
		int lata = findLata(fname);
		if(lata != 0) {
			Field field = null;
    		try {
				field = modelClass.getDeclaredField("LL");
			} catch (NoSuchFieldException | SecurityException e) {
				e.printStackTrace();
			}
    		try {
    			field.setAccessible(true);
				field.set(modelObject, lata);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("Not 'LATA' line in data file!");
			return this;
		}
		try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
            String line;
            while ((line = br.readLine()) != null) {
            	String[] parts = line.split("\\s+");
            	if(parts[0].equals("LATA")) continue;
            	Field field = null;
        		try {
    				field = modelClass.getDeclaredField(parts[0]);
    			} catch (NoSuchFieldException | SecurityException e) {
    				e.printStackTrace();
    			}
        		Bind annot = field.getAnnotation(Bind.class);
        		if(annot == null) continue;
        		double[] data = new double[lata];
                for(int i = 0; i < lata; ++i) {
                	if(i < parts.length - 1)
                		data[i] = Double.parseDouble(parts[i + 1]);
                	else
                		data[i] = Double.parseDouble(parts[parts.length - 1]);
                }
                try {
                	field.setAccessible(true);
					field.set(modelObject, data);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					e.printStackTrace();
				}
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		return this;
	}
	
	public Controller runModel() {
		try {
			Method method = modelClass.getMethod("run");
			try {
				method.invoke(modelObject);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
		return this;
	}
	
	public Controller runScriptFromFile(String fname) {
		setBindings();
		try {
			engine.eval(new FileReader(fname));
		} catch (ScriptException e) {
			e.printStackTrace();
		}	catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		getValuesFromBindings();
		return this;
	}
	
	public Controller runScript(String script) {
		setBindings();
		try {
		      engine.eval(script);
		    } catch (ScriptException e) {
		      e.printStackTrace();
		    }
		getValuesFromBindings();
		return this;
	}

	public String getResultsAsTsv() {
		String results = "";
		for(Field field : modelClass.getDeclaredFields()) {
			field.setAccessible(true);
			Bind annot = field.getAnnotation(Bind.class);
    		if(annot == null) continue;
			String line = "";
			try {
				line = formatResultLine(field.getName(), field.get(modelObject));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
			results += line;
		}
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		for(String fieldName : bindings.keySet()) {
			if(fieldName.length() == 1) {
				char sign = fieldName.charAt(0);
				if(sign <= 'z' && sign >= 'a') continue;
			}
			if(!containsField(fieldName)) {
				Object value = bindings.get(fieldName);
				results += formatResultLine(fieldName, value);
			}
		}
		return results;
	}
	
	private int findLata(String fname) {
		try (BufferedReader br = new BufferedReader(new FileReader(fname))) {
            String line;
            while ((line = br.readLine()) != null) {
            	String[] parts = line.split("\\s+");
            	if(parts[0].equals("LATA")) {
            		return parts.length - 1;
            	} 
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		return 0;
	}

	private void setBindings() {
		for(Field field : modelClass.getDeclaredFields()) {
			field.setAccessible(true);
			Bind annot = field.getAnnotation(Bind.class);
    		if(annot == null) continue;
			String name = field.getName();
			try {
				Object value = field.get(modelObject);
				engine.put(name, value);
				
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void getValuesFromBindings() {
		Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
		for(Field field : modelClass.getDeclaredFields()) {
			field.setAccessible(true);
			Bind annot = field.getAnnotation(Bind.class);
    		if(annot == null) continue;
			String name = field.getName();
			try {
				Object value = bindings.get(name);
				field.set(modelObject, value);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}
	
	private String formatResultLine(String name, Object value) {
		String line = name;
		if(value != null) {
			if(value.getClass().isArray()) {
				double[] values = (double[]) value;
				for(Object o : values) {
					line += "\t";
					line += o;
				}
			} else {
				line += "\t";
				line += value;
			}
		}
		line += "\n";
		return line;
		
	}

	private boolean containsField(String fieldName) {
	    for (Field field : modelClass.getDeclaredFields()) {
	        if (field.getName().equals(fieldName)) {
	            return true;
	        }
	    }
	    return false;
	}
}
