package garretreichenbach.taskprocessor.lua;

import lombok.extern.slf4j.Slf4j;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;

import static org.luaj.vm2.LuaValue.NIL;

@Slf4j
public class LuaEnvironment {

	private static final String[] WHITELISTED_LIBS = {"base", "string", "table", "math", "package", "bit32"};

	private static class ReadOnlyLuaTable extends LuaTable {
		public ReadOnlyLuaTable(LuaValue table) {
			presize(table.length(), 0);
			for(Varargs n = table.next(NIL); !n.arg1().isnil(); n = table.next(n.arg1())) {
				LuaValue key = n.arg1();
				LuaValue value = n.arg(2);
				super.rawset(key, value.istable() ? new ReadOnlyLuaTable(value) : value);
			}
		}

		public LuaValue setmetatable(LuaValue metatable) {
			return error("table is read-only");
		}

		public void set(int key, LuaValue value) {
			error("table is read-only");
		}

		public void rawset(int key, LuaValue value) {
			error("table is read-only");
		}

		public void rawset(LuaValue key, LuaValue value) {
			error("table is read-only");
		}

		public LuaValue remove(int pos) {
			return error("table is read-only");
		}
	}

	public static LuaValue create(String script) {
		try {
			Globals globals = new Globals();
			globals.load(new JseBaseLib());
			globals.load(new PackageLib());
			globals.load(new StringLib());
			globals.load(new TableLib());
			globals.load(new JseMathLib());
			globals.load(new Bit32Lib());
			LuaC.install(globals);
			LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);

			//Security Patches
			for(LuaValue key : globals.keys()) {
				LuaValue value = globals.get(key);
				if(value instanceof LuaTable table) {
					if(table.getmetatable() != null) table.setmetatable(new ReadOnlyLuaTable(table.getmetatable()));
				}
				//Check for whitelisted libs
				boolean whitelisted = false;
				for(String lib : WHITELISTED_LIBS) {
					if(key.tojstring().equals(lib)) {
						whitelisted = true;
						break;
					}
				}
				if(!whitelisted) globals.set(key, NIL);
			}
			//Todo: Scan for suspicious code
			assignFunctions(globals);
			LuaValue load = globals.load(script);
			if(load.isnil()) {
				log.error("Failed to load Lua script: {}", script);
				return null;
			}
			log.info("Loaded Lua script: {}", script);
			return load;
		} catch(Exception exception) {
			log.error("Failed to create Lua environment: {}", exception.getMessage(), exception);
			return null;
		}
	}

	/**
	 * Assigns functions to the Lua environment.
	 * @param globals The Lua globals table.
	 */
	private static void assignFunctions(Globals globals) {
		globals.set("print", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue arg) {
				log.info("Lua print: {}", arg.tojstring());
				return NIL;
			}
		});
		//Todo: Add more functions and values
	}
}
